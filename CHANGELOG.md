# Changelog

## v1.0.0-rc1 (12/11/2026)

Tag anotada no commit `f4a706f`, com CI aprovado ("Qualidade #23"). Decisão de qualidade: liberar com ressalvas (`docs/decisao-qualidade.md`).

Primeira versão candidata. Itens integrados desde a versão inicial de 25/10/2026:

### Funcionalidades
- Relatório de utilização por período: `GET /api/relatorios/utilizacao?inicio=&fim=`, com horas por recurso, horas por professor e conflitos evitados (RF12).
- Histórico auditável de estados das reservas: tabela `booking_history` imutável e `GET /api/bookings/{id}/history` (RF10). Motivo opcional em `POST /api/bookings/{id}/state` e instante via `Clock`.
- Parâmetro obrigatório ausente passa a responder 400 com a mensagem padrão de validação.

### Testes e qualidade
- Matriz de autorização executável: 30 operações × 4 perfis, posse e respostas de erro (`AutorizacaoMatrizIT`).
- Testes com oráculo calculado à mão para o relatório de utilização (`RelatorioUtilizacaoIT`).
- Testes do histórico com Clock fixo, rollback e imutabilidade (`HistoricoEstadosTest`, `HistoricoEstadosIT`).
- Entrega da notificação com WireMock (`NotificacaoEntregaIT`), caminhos de erro (`CasosDeErroIT`) e filtros de pesquisa (`FiltrosRecursosIT`).
- Teste de regressão do defeito D04.
- Suíte: de 44 para 211 testes; cobertura de 93,39%/77,85% para 97,63%/92,61% (linhas/branches).

### CI
- Runner fixo `ubuntu-24.04` (correção do defeito D05).
- Resumo dos testes e anotação de cada teste que falhar no PR.
- Job `load`: teste de carga com JMeter (10, 50 e 100 usuários).

### Documentação
- `docs/seguranca/matriz-autorizacao.md`, `docs/testes/mutacao-manual.md`, `docs/performance/resultado-01.md`, `docs/status-requisitos.md`, `docs/evidencias/README.md`, `docs/entrega/checklist.md`.
- Relatório de qualidade, RTM e defeitos atualizados com as evidências do CI; arquivos renomeados para `docs/rtm.md`, `docs/defeitos/` e `docs/relatorio-qualidade.md`.

### Organização
- Maven Wrapper (`mvnw`, `mvnw.cmd`): não é mais preciso instalar o Maven; README, documentos e CI usam `./mvnw`.
- Modelo de PR, modelo de defeito, `CODEOWNERS` e índice `docs/README.md`.

### Segurança
- Consultas de `GET /resources`, `GET /bookings` e do relatório de utilização reescritas sem SQL concatenado (defeito D09, apontado pelo SonarCloud).

### Pendências conhecidas
- `Rules.overlaps` não usado pela aplicação (D06).
