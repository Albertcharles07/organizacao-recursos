# RTM: requisito, risco, teste e evidência

Atualizada em **05/11/2026**. Cada linha liga o requisito ao risco, à implementação, ao teste e à evidência de execução. **Mapeado não significa aprovado**: a coluna de evidência só diz "passou" quando o teste executou no CI, e a coluna de mutação diz se a quebra proposital da regra foi detectada (`docs/testes/mutacao-manual.md`).

Execução de referência: GitHub Actions, commit `be6fda0` (branch `auditoria/fechamento`, PR de validação no fork `lucasrmagalhaess/organizacao-recursos`): **208 testes, 0 falhas; cobertura de 97,65% das linhas e 92,53% dos branches; E2E e carga aprovados.** A execução final deve ser repetida no repositório do grupo, no commit da tag `v1.0.0-rc1`.

| ID | Requisito | Risco | Implementação | Teste | Mutação | Evidência |
| --- | --- | --- | --- | --- | --- | --- |
| RF01* | Autenticação e autorização por perfil | Acesso indevido | `SecurityConfig`, `ActiveAccountFilter`, checagens no `ApiController` | `AutorizacaoMatrizIT` (matriz `docs/seguranca/matriz-autorizacao.md`: 30 operações × 4 perfis, posse, erros); `CasosDeErroIT` (login 401, logout, conta desativada); `BlackBoxIT` | 04, 06 e 07 detectadas | Passou em `be6fda0` |
| RF02 | Cadastro e consulta de salas, professores e materiais | Cadastro inconsistente | `/resources`, `/users` | `ResourceApiIT.adminResourceAndUserFlows`; `AutorizacaoMatrizIT` M05 a M11; `CasosDeErroIT` (usuário duplicado, senha curta); E2E | — | Passou em `be6fda0` |
| RF03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Disponibilidade falsa | `GET /resources` | `FiltrosRecursosIT`; `ResourceApiIT.availabilityFiltersAndPrivacy` | — | Passou em `be6fda0` |
| RF04 | Criar, alterar e cancelar reservas | Perda de dados | `BookingService.save` e `transition` | `cancelReleasesResources`; `failedEditRollsBack`; `ownerIsEnforced`; `CasosDeErroIT` (rejeição, não comparecimento) | 07 detectada | Passou em `be6fda0` |
| RF05* | Sobreposição de sala, material e professor | Recurso em dois lugares | Restrição `EXCLUDE USING gist` em `allocation` | `teacherCannotBeInTwoRooms`; `adjacentReservationsAreAccepted`; `RulesTest.overlap`** | 01 e 02 detectadas | Passou em `be6fda0` |
| RF06* | Proteção contra dupla reserva simultânea | Dupla reserva | Bloqueio `for update` e restrição de exclusão | `simultaneousRequestsAcceptOnlyOne`; carga JMeter com 10, 50 e 100 usuários | 02 detectada | Passou em `be6fda0`; carga: `docs/performance/resultado-01.md` |
| RF07* | Aprovação obrigatória para recursos restritos | Uso sem autorização | Estado inicial SOLICITADA e checagem de perfil | `restrictedApprovalAndAudit`; `bddRestrictedResource`; `AutorizacaoMatrizIT` M16 e M17 | 03 e 04 detectadas | Passou em `be6fda0` |
| RF08* | Bloqueio de recursos em manutenção | Reserva de recurso indisponível | Alocação sem `booking_id` na mesma restrição de exclusão | `maintenanceAndReservationsShareExclusion`; `maintenanceCannotOverrideBooking`; `AutorizacaoMatrizIT` M23 a M25 | 05 detectada | Passou em `be6fda0` |
| RF09 | Retirada e devolução de materiais | Material não devolvido | `material_movement` | `startedBookingAndMaterialLifecycle`; `AutorizacaoMatrizIT` M21 e M22 | — | Passou em `be6fda0` |
| RF10* | Histórico auditável de mudanças | Alteração sem autoria | `audit` e `booking_history`, ambas imutáveis por trigger; `GET /bookings/{id}/history` | `HistoricoEstadosTest` (Clock fixo); `HistoricoEstadosIT` (rollback, imutabilidade, 403); `auditCannotBeRewritten` | 08 detectada | Passou em `be6fda0` |
| RF11 | Notificação simulada ou integração externa | Falha externa compromete o fluxo | `notification`, `NotificationGateway`, `NotificationDelivery` | `NotificationGatewayTest`; `NotificacaoEntregaIT` (WireMock: ENVIADA, FALHOU, queda) | — | Passou em `be6fda0` |
| RF12 | Relatório de utilização, carga horária e conflitos evitados | Indicadores incorretos | `GET /relatorios/utilizacao` | `RelatorioUtilizacaoIT` (oráculo calculado à mão); `AutorizacaoMatrizIT` M29 | 09 detectada | Passou em `be6fda0` |
| RF13 | Interface responsiva e mensagens compreensíveis | Operação incompreensível | `static/` e `ApiErrors` | `e2e/reservas.spec.js` (fluxo e layout móvel); `AutorizacaoMatrizIT.entradasInvalidasRetornam400SemDetalhesInternos` | — | Passou em `be6fda0` |
| RF14 | Documentação da API e dos fluxos | Uso sem contrato | `docs/API.md`, `README.md` | `BlackBoxIT` segue o fluxo documentado; revisão por colega | — | `BlackBoxIT` passou em `be6fda0`; revisão no PR |
| RC01* | Término posterior ao início | Intervalo inválido | `Rules.interval` e `CHECK(end_at>start_at)` | `RulesTest.invalidIntervals`; `RelatorioUtilizacaoIT.periodoInvalidoRetorna400` | 10 detectada | Passou em `be6fda0` |
| RC02* | Reserva iniciada não pode ser excluída | Perda de histórico | Sem rota de exclusão; cancelamento recusado após o início | `startedBookingAndMaterialLifecycle`; `HistoricoEstadosIT.reservaIniciadaNaoPodeSerCanceladaNemExcluida` | — | Passou em `be6fda0` |
| RC03* | Toda mudança de estado gera auditoria | Rastreio incompleto | `BookingService.history` na mesma transação | `HistoricoEstadosIT.cicloCompletoGeraUmRegistroPorMudanca`; `falhaNaTransacaoNaoDeixaHistoricoOrfao` | 08 detectada | Passou em `be6fda0` |
| RNF01 | 80% de linhas e 70% de branches | Testes insuficientes | JaCoCo e perfil `quality` | `jacoco:check@coverage-gate` | — | 97,65% e 92,53% em `be6fda0` |
| RNF02 | Zero bugs e vulnerabilidades críticas | Falha não detectada | SonarCloud com quality gate | Passo do SonarCloud no CI | — | **Pendente**: falta `SONAR_TOKEN` (defeito P03) |
| RNF03 | Desempenho e robustez sob concorrência | Lentidão ou falha sob carga | JMeter, timeout do webhook | Job `load`; `NotificacaoEntregaIT.apiExternaForaDoArMarcaFalhou` | 02 detectada | p95 de 487 ms com 100 usuários, 1 reserva por rodada (`docs/performance/resultado-01.md`) |

`*` = requisito crítico. \*\* `Rules.overlaps` não é chamado pelo código da aplicação (defeito D06): o `RulesTest.overlap` documenta a regra, mas a proteção real de RF05 é a restrição do banco.

**Cobertura de rastreabilidade crítica:** 100% dos requisitos críticos com teste executado no CI e mutação detectada. Pendente para a liberação: RNF02 (SonarCloud) e a repetição do CI no repositório do grupo no commit da tag.
