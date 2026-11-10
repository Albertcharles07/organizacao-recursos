# API e fluxos públicos

Base: `http://localhost:8080/api`. JSON UTF-8; instantes ISO-8601 com offset, como `2026-12-01T14:00:00-03:00`. Os horários de exemplo devem ser substituídos por datas futuras.

## Autenticação de sessão e CSRF

1. `GET /csrf` retorna `token` e `headerName` e estabelece sessão. Preserve os cookies.
2. `POST /login`, Content-Type `application/x-www-form-urlencoded`, campos `username` e `password`, enviando o token no header retornado.
3. Sucesso: 204. Busque **novo** token com `GET /csrf` após login, pois a autenticação renova a proteção.
4. Em toda mutação, envie cookie de sessão e header CSRF. `POST /logout` encerra a sessão.
5. `GET /me` retorna username e authorities. Conta desativada perde acesso no próximo request.

## Endpoints

| Método | Caminho | Entrada / comportamento |
|---|---|---|
| GET | `/resources` | Filtros `type`, `capacity`, `location`, `competence`, `start`, `end`; início e fim devem vir juntos. `includeInactive=true` é exclusivo de ADMIN |
| POST | `/resources` | Cadastro (ADMIN); retorna `{id}` |
| PUT | `/resources/{id}` | Edição/desativação (ADMIN), sem alocações futuras ativas |
| GET/POST | `/users` | Lista sem hashes / cria usuário (ADMIN) |
| PUT | `/users/{id}/enabled` | `{ "enabled": false }`; não desativar a própria conta |
| GET | `/bookings` | Lista conforme escopo do perfil |
| POST | `/bookings` | Cria reserva; retorna `{id}` |
| PUT | `/bookings/{id}` | Substitui dados da reserva própria futura; retorna `{id}` |
| POST | `/bookings/{id}/state` | `{ "state": "APROVADA", "reason": "motivo opcional" }`; regras em REGRAS.md. Toda mudança grava o histórico da reserva |
| GET | `/bookings/{id}/history` | Histórico de estados da reserva: usuário, instante, estado anterior, estado novo e motivo (ADMIN/RESPONSAVEL). Somente leitura |
| POST | `/bookings/{id}/movements` | `{ "resource": 3, "kind": "RETIRADA" }` ou DEVOLUCAO |
| GET | `/movements` | Registro de retiradas e devoluções (RESPONSAVEL) |
| GET/POST | `/maintenance` | Lista bloqueios ativos / cria bloqueio (ADMIN) |
| DELETE | `/maintenance/{id}` | Desativa bloqueio sem apagar histórico (ADMIN) |
| GET | `/audit` | Últimas 500 entradas (ADMIN/RESPONSAVEL) |
| GET | `/notifications` | Últimas 100 notificações próprias |
| GET | `/reports` | Utilização por recurso e total de conflitos (ADMIN/RESPONSAVEL) |
| GET | `/relatorios/utilizacao` | `inicio` e `fim` obrigatórios (ISO-8601). Horas por recurso, horas por professor e conflitos evitados no período (ADMIN/RESPONSAVEL) |

### Cadastro de recurso

```json
{"name":"Projetor 01","type":"MATERIAL","capacity":1,"location":"Bloco A","competence":"Projeção HDMI","restricted":true,"active":true}
```

Tipos: SALA, PROFESSOR, MATERIAL. Para docente, competence contém disciplina/habilidade; localização pode ser a unidade/campus.

### Criação/alteração de reserva

```json
{"title":"Aula de Java","start":"2026-12-01T14:00:00-03:00","end":"2026-12-01T16:00:00-03:00","resources":[1,2,3]}
```

Até 30 IDs, positivos e existentes. Duplicatas são removidas. Para alterar, envie a lista completa novamente. ID vem do banco; nunca é gerado pelo navegador.

### Bloqueio e usuário

```json
{"resource":1,"start":"2026-12-02T08:00:00-03:00","end":"2026-12-02T12:00:00-03:00","reason":"Manutenção elétrica"}
```

```json
{"username":"coordenacao","password":"substituir-por-senha-propria","role":"RESPONSAVEL"}
```

### Relatório de utilização por período

`GET /relatorios/utilizacao?inicio=2026-12-01T08:00:00-03:00&fim=2026-12-01T18:00:00-03:00`

```json
{"inicio":"2026-12-01T11:00:00Z","fim":"2026-12-01T21:00:00Z",
 "recursos":[{"id":1,"name":"Sala 101","type":"SALA","horas":5.0}],
 "professores":[{"id":3,"name":"Prof. Ana","type":"PROFESSOR","horas":3.0}],
 "conflitosEvitados":2}
```

Regras de cálculo:

- Contam as reservas APROVADA, EM_USO e CONCLUIDA. SOLICITADA, CANCELADA, REJEITADA e NAO_COMPARECEU não contam.
- Só entra a parte da reserva dentro do período `[inicio, fim)`: uma reserva das 17h às 20h num período que termina às 18h conta 1 hora.
- `recursos` lista salas e materiais; `professores` lista os docentes. Recursos sem uso aparecem com `horas: 0.0`.
- `conflitosEvitados` conta as tentativas de reserva recusadas por sobreposição (HTTP 409) feitas dentro do período.
- `fim` anterior ou igual a `inicio`, ou parâmetro ausente: 400. Solicitante: 403. Sem login: 401.

### Histórico de estados da reserva

`GET /bookings/7/history`

```json
[{"id":1,"booking_id":7,"actor":"ana","at":"2026-11-05T22:10:00.000+00:00","previous_state":null,"new_state":"APROVADA","reason":"Reserva criada"},
 {"id":2,"booking_id":7,"actor":"ana","at":"2026-11-05T22:30:00.000+00:00","previous_state":"APROVADA","new_state":"CANCELADA","reason":"Aula remarcada"}]
```

- Criar ou alterar uma reserva e cada mudança de estado gravam uma linha, na mesma transação da mudança: se a operação falha, nenhuma linha fica gravada.
- O instante vem do `Clock` da aplicação. O motivo vem do campo `reason` da mudança de estado; sem motivo, grava "Não informado".
- O histórico não pode ser alterado nem apagado: não há rota para isso e o banco recusa `UPDATE` e `DELETE` na tabela `booking_history`.

### Respostas

Operações com retorno: 200. Mutações sem conteúdo: 200 vazio; login/logout: 204. Erros de domínio têm `{ "status": 409, "message": "Horário indisponível: existe reserva ou manutenção para um dos recursos." }`.

- 400: formato/regra inválida, recurso ausente, transição impossível.
- 401: sessão ausente, login incorreto ou conta desativada.
- 403: perfil insuficiente, dono diferente ou CSRF inválido.
- 409: sobreposição, duplicata ou integridade incompatível.
- 500: falha inesperada, sem detalhes de SQL/stacktrace expostos.

Erros anteriores ao controller (Spring Security) usam o formato de erro padrão do Spring; clientes devem lidar com o status sem depender da presença de `message`. Não existe endpoint de exclusão de reservas.

## Webhook opcional

Defina `NOTIFICATION_URL` no ambiente (e repasse em compose.yaml se usar Compose). Sem URL, a notificação fica SIMULADA. Com URL, após o commit é enviado POST com `{ "bookingId": 123 }`, timeout de conexão 3s e total 5s. Não envia dados pessoais. 2xx marca ENVIADA; falha marca FALHOU. Não há reenvio automático nesta versão; a notificação interna permanece acessível.

Use URL fixa administrada pela equipe, nunca URL recebida do solicitante. WireMock testa payload, falha e não seguimento de redirects.
