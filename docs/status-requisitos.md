# Status dos requisitos

Diagnóstico de **05/11/2026** (Aula 11), comparando a especificação com o código, os testes e a RTM (`docs/rtm.md`). Um item só é **feito** quando existe teste que falharia se a regra fosse quebrada; nas regras críticas isso foi confirmado pela mutação manual (`docs/testes/mutacao-manual.md`). Execução de referência: CI do commit `2801221`, 208 testes passando, E2E e carga aprovados.

## Requisitos funcionais

| Requisito | Status | Evidência (teste) | Risco se ficar de fora |
| --- | --- | --- | --- |
| RF01 Autenticação e autorização por perfil | Feito | `AutorizacaoMatrizIT` (30 operações × 4 perfis, posse), `CasosDeErroIT` (login 401, logout); mutações 04, 06 e 07 | Limitador de nota: falha de autorização |
| RF02 Cadastro e consulta de salas, professores e materiais | Feito | `ResourceApiIT.adminResourceAndUserFlows`; `AutorizacaoMatrizIT` M05 a M08 | Recursos inconsistentes |
| RF03 Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Feito | `FiltrosRecursosIT` (tipo, capacidade mínima, localização e competência por trecho, combinação); `ResourceApiIT.availabilityFiltersAndPrivacy` (disponibilidade) | Filtro errado sem ninguém perceber |
| RF04 Criação, alteração e cancelamento | Feito | `cancelReleasesResources`, `failedEditRollsBack`, `ownerIsEnforced`; mutação 07 | Perda de dados da reserva |
| RF05 Sobreposição de horários, inclusive do professor | Feito | `teacherCannotBeInTwoRooms`, `adjacentReservationsAreAccepted`; mutações 01 e 02 | Recurso em dois lugares ao mesmo tempo |
| RF06 Proteção contra dupla reserva simultânea | Feito | `simultaneousRequestsAcceptOnlyOne`; carga com 10, 50 e 100 usuários (`docs/performance/resultado-01.md`); mutação 02 | Limitador de nota: dupla reserva |
| RF07 Aprovação obrigatória para recursos restritos | Feito | `restrictedApprovalAndAudit`, `bddRestrictedResource`; mutações 03 e 04 | Uso de recurso restrito sem autorização |
| RF08 Bloqueio de recursos em manutenção | Feito | `maintenanceAndReservationsShareExclusion`, `maintenanceCannotOverrideBooking`; mutação 05 | Reserva de recurso indisponível |
| RF09 Retirada e devolução de materiais | Feito | `startedBookingAndMaterialLifecycle`; `AutorizacaoMatrizIT` M21 e M22 | Material sem devolução registrada |
| RF10 Histórico auditável de mudanças | Feito | `HistoricoEstadosTest`, `HistoricoEstadosIT` (rollback, imutabilidade, 403); mutação 08 | Mudança sem autoria |
| RF11 Notificação simulada ou integração externa | Feito | `NotificationGatewayTest`, `NotificacaoEntregaIT` (WireMock: ENVIADA, FALHOU) | Falha externa derruba o fluxo |
| RF12 Relatório de utilização, horas e conflitos evitados | Feito | `RelatorioUtilizacaoIT` (oráculo calculado à mão); mutação 09 | Indicadores incorretos |
| RF13 Interface responsiva e mensagens compreensíveis | Feito | `e2e/reservas.spec.js` (fluxo e layout móvel); `AutorizacaoMatrizIT.entradasInvalidasRetornam400SemDetalhesInternos` | Usuário não entende o erro |
| RF14 Documentação da API e dos fluxos | Feito | `docs/API.md` (inclui relatório e histórico); `BlackBoxIT` segue o fluxo documentado | Uso da API sem contrato |

## Regras críticas e requisitos não funcionais

| Item | Status | Evidência | Risco |
| --- | --- | --- | --- |
| Término posterior ao início | Feito | `RulesTest.invalidIntervals`, `CHECK(end_at>start_at)`; mutação 10 | Intervalo inválido gravado |
| Reserva iniciada não pode ser excluída nem cancelada | Feito | `startedBookingAndMaterialLifecycle`, `HistoricoEstadosIT.reservaIniciadaNaoPodeSerCanceladaNemExcluida` | Perda de histórico |
| Toda mudança de estado gera auditoria | Feito | `HistoricoEstadosIT.cicloCompletoGeraUmRegistroPorMudanca`, `falhaNaTransacaoNaoDeixaHistoricoOrfao`; mutação 08 | Rastreio incompleto |
| Cobertura 80% linhas e 70% branches | Feito | 97,65% e 92,53% no CI do commit `2801221` | Nota de cobertura |
| Desempenho e robustez sob concorrência | Feito | `docs/performance/resultado-01.md` (p95 até 487 ms com 100 usuários) | Sem evidência de desempenho |
| Zero bugs e vulnerabilidades críticas (SonarCloud) | Feito | Quality gate aprovado na `main` (06/11/2026); 2 issues médias de segurança corrigidas (D09), nota A a confirmar | Vulnerabilidade sem detecção |
| Revisão por pares | **Parcial** | PRs abertos aguardando revisão | Critério de autoria e revisão |

## Pendências, em ordem de impacto

1. **Pipeline verde (limitador de nota):** feito em 06/11/2026: repositório público, SonarCloud no CI e os três checks verdes no PR #4. Falta confirmar a nota de segurança A depois da correção do D09.
2. **Aplicação executável para o professor (limitador de nota):** repositório público e checklist de clone limpo (`docs/entrega/checklist.md`) feito por alguém que siga só o README.
3. **Integrar os PRs na `main`** com revisão de outro integrante, na ordem da fila.
4. **Repetir no repositório do grupo** o CI completo no commit da tag `v1.0.0-rc1`, porque as execuções de 05/11 com 202 testes foram no fork de validação.

## Divisão sugerida até a semana 46

| Pendência | Responsável |
| --- | --- |
| Revisão e merge dos PRs | Integrantes que não abriram o PR |
| Checklist de clone limpo | Integrante com Docker funcionando |
| Leitura do SonarCloud e registro de defeitos | Auditoria (Lucas) |
| Tag, pacote de evidências e decisão de qualidade | Toda a equipe na Aula 14 |
