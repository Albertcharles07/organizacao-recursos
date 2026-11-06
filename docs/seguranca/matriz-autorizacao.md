# Matriz de autorização

Esta matriz define, para cada operação da API, a resposta HTTP esperada em cada perfil. Ela é a fonte do teste
`src/test/java/br/edu/recursos/AutorizacaoMatrizIT.java`, que lê **as linhas desta tabela** e executa cada célula
com login real (`POST /api/login`) de um usuário de cada perfil. Uma célula alterada aqui muda o teste; uma célula
removida faz o teste falhar, porque todo cenário do código precisa ter uma linha correspondente.

Usuários do teste: `ana` (Solicitante, dona das reservas do cenário), `gestor` (Responsável) e `admin` (Administrador).
Todas as requisições enviam token CSRF válido, para que o resultado reflita a autorização e não a proteção CSRF.

## Matriz perfil × operação

| ID | Operação | Método e rota | Anônimo | Solicitante | Responsável | Administrador | Observação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| M01 | Tela inicial | GET / | 200 | 200 | 200 | 200 | Público |
| M02 | Obter token CSRF | GET /api/csrf | 200 | 200 | 200 | 200 | Público |
| M03 | Encerrar sessão | POST /api/logout | 204 | 204 | 204 | 204 | Público; sem sessão também responde 204 |
| M04 | Dados do usuário logado | GET /api/me | 401 | 200 | 200 | 200 | |
| M05 | Consultar recursos ativos | GET /api/resources | 401 | 200 | 200 | 200 | |
| M06 | Consultar recursos inativos | GET /api/resources?includeInactive=true | 401 | 403 | 403 | 200 | |
| M07 | Cadastrar recurso | POST /api/resources | 401 | 403 | 403 | 200 | |
| M08 | Editar recurso | PUT /api/resources/{id} | 401 | 403 | 403 | 200 | Recurso sem alocação futura |
| M09 | Listar usuários | GET /api/users | 401 | 403 | 403 | 200 | |
| M10 | Criar usuário | POST /api/users | 401 | 403 | 403 | 200 | |
| M11 | Ativar ou desativar usuário | PUT /api/users/{id}/enabled | 401 | 403 | 403 | 200 | |
| M12 | Listar reservas | GET /api/bookings | 401 | 200 | 200 | 200 | Solicitante vê só as próprias (teste de posse) |
| M13 | Criar reserva | POST /api/bookings | 401 | 200 | 200 | 200 | **A confirmar**: ver divergência DV01 |
| M14 | Alterar reserva | PUT /api/bookings/{id} | 401 | 200 | 403 | 403 | Só o dono altera; reserva da `ana` |
| M15 | Cancelar reserva | POST /api/bookings/{id}/state CANCELADA | 401 | 200 | 200 | 403 | Dono ou Responsável; reserva da `ana` |
| M16 | Aprovar recurso restrito | POST /api/bookings/{id}/state APROVADA | 401 | 403 | 200 | 403 | Só o Responsável aprova |
| M17 | Rejeitar solicitação | POST /api/bookings/{id}/state REJEITADA | 401 | 403 | 200 | 403 | |
| M18 | Iniciar uso | POST /api/bookings/{id}/state EM_USO | 401 | 403 | 200 | 403 | Reserva aprovada já iniciada |
| M19 | Registrar não comparecimento | POST /api/bookings/{id}/state NAO_COMPARECEU | 401 | 403 | 200 | 403 | Reserva aprovada já iniciada |
| M20 | Concluir uso | POST /api/bookings/{id}/state CONCLUIDA | 401 | 403 | 200 | 403 | Reserva em uso, sem material pendente |
| M21 | Registrar retirada de material | POST /api/bookings/{id}/movements | 401 | 403 | 200 | 403 | Reserva em uso com material |
| M22 | Listar movimentações | GET /api/movements | 401 | 403 | 200 | 403 | |
| M23 | Listar manutenções | GET /api/maintenance | 401 | 200 | 200 | 200 | Ver observação OB02 |
| M24 | Bloquear recurso para manutenção | POST /api/maintenance | 401 | 403 | 403 | 200 | |
| M25 | Remover bloqueio | DELETE /api/maintenance/{id} | 401 | 403 | 403 | 200 | |
| M26 | Consultar auditoria | GET /api/audit | 401 | 403 | 200 | 200 | |
| M27 | Consultar notificações próprias | GET /api/notifications | 401 | 200 | 200 | 200 | Cada usuário vê só as próprias |
| M28 | Relatório de utilização | GET /api/reports | 401 | 403 | 200 | 200 | |

## Regras de posse

Testadas em `AutorizacaoMatrizIT.solicitanteNaoMexeNaReservaDeOutro`, com `bia` (Solicitante) agindo sobre uma
reserva da `ana`:

| Caso | Esperado |
| --- | --- |
| `bia` altera a reserva da `ana` pelo id (PUT /api/bookings/{id}) | 403, reserva inalterada |
| `bia` cancela a reserva da `ana` pelo id (POST /api/bookings/{id}/state CANCELADA) | 403, reserva continua APROVADA |
| `bia` lista reservas (GET /api/bookings) | A reserva da `ana` não aparece |

## Validação de entrada e segurança das respostas de erro

Testadas em `AutorizacaoMatrizIT.entradasInvalidasRetornam400SemDetalhesInternos`. Toda resposta de erro deve ter
corpo `{"status", "message"}` com mensagem compreensível e **não** pode conter stack trace, SQL ou nome de classe
interna.

| Caso | Esperado |
| --- | --- |
| Reserva com término antes do início | 400, "O término deve ser posterior ao início." |
| Recurso com capacidade negativa | 400, mensagem de revisão dos campos |
| Reserva com campos ausentes (`{}`) | 400, mensagem de revisão dos campos |
| JSON malformado | 400, mensagem de revisão dos campos |
| Id não numérico na rota | 400, mensagem de revisão dos campos |
| Reserva sobreposta | 409, "Horário indisponível…" |

## Divergências e riscos encontrados na análise do código

| ID | Tipo | Descrição | Situação |
| --- | --- | --- | --- |
| DV01 | Decisão de escopo | A especificação diz que o Solicitante cria reservas, mas o código permite que Responsável e Administrador também criem (M13). A matriz registra o comportamento atual. | A equipe decide: manter (e registrar em ADR) ou restringir e mudar M13 para 403 |
| RS01 | Risco de projeto | O `SecurityConfig` só exige "estar logado" para `/api/**`. **Nenhuma rota tem regra por perfil na configuração de segurança**: toda checagem de perfil está dentro do `ApiController` (métodos `admin()`, `responsible()` e `has()`). Um endpoint novo sem essa chamada fica aberto a qualquer usuário logado. | Mitigado por esta matriz: um endpoint novo sem linha aqui não é testado, então toda rota nova deve ganhar uma linha |
| OB01 | Observação | As respostas de erro usam um corpo próprio `{"status","message"}`, e não o formato padrão `ProblemDetail` (RFC 9457) do Spring. Atende ao requisito de não expor detalhes internos. | Melhoria opcional |
| OB02 | Observação | `GET /api/maintenance` não verifica perfil: qualquer usuário logado vê os bloqueios e o motivo. Parece intencional (o Solicitante precisa saber por que um recurso está indisponível). | Confirmar com a equipe |

Se alguma célula falhar no CI, a falha vira um defeito registrado em `docs/DEFEITOS.md` **antes** da correção, e a
correção vai em commit próprio que cita o defeito. Nenhuma célula deve ser removida ou alterada só para o teste passar.
