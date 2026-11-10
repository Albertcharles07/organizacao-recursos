# Mutação manual das regras críticas

Executada em **05/11/2026** (Aula 12). Para cada regra crítica, a regra foi quebrada de propósito no código de produção, numa branch descartável, e a suíte completa rodou no GitHub Actions. **Resultado: 10 de 10 mutações detectadas.** Em todas, pelo menos um teste ligado à regra ficou vermelho.

Nenhuma mutação entrou no código: cada uma ficou numa branch `mut/*` própria, com PR de rascunho fechado sem merge. Base de todas as mutações: commit `0170b7b` (branch `auditoria/fechamento`), em que a suíte tem 202 testes e todos passam.

## Resultado

| # | Regra crítica | Quebra feita | Testes que falharam | Detectada? |
| --- | --- | --- | --- | --- |
| 01 | Sobreposição: reservas adjacentes são permitidas (intervalo `[início, fim)`) | `V1__schema.sql`: `tstzrange(start_at,end_at,'[)')` → `'[]'` | `ResourceApiIT.adjacentReservationsAreAccepted`, `ResourceApiIT.failedEditRollsBack`, `HistoricoEstadosIT.alteracaoRecusadaNaoGravaHistorico` (3) | Sim |
| 02 | Dupla reserva e sobreposição de sala, professor e material | `V1__schema.sql`: restrição `EXCLUDE USING gist(...)` removida | `ResourceApiIT.simultaneousRequestsAcceptOnlyOne` ("duplicate element: 200": duas reservas aceitas), `teacherCannotBeInTwoRooms`, `maintenanceAndReservationsShareExclusion`, `maintenanceCannotOverrideBooking`, `failedEditRollsBack`, `RelatorioUtilizacaoIT.conflitoRecusadoPelaApiEntraNoPeriodoDaTentativa`, `HistoricoEstadosIT.alteracaoRecusadaNaoGravaHistorico`, `AutorizacaoMatrizIT.entradasInvalidasRetornam400SemDetalhesInternos` (8) | Sim |
| 03 | Recurso restrito começa SOLICITADA e exige aprovação | `BookingService.save`: `restricted ? "SOLICITADA" : "APROVADA"` → sempre `"APROVADA"` | 11 testes, entre eles `ResourceApiIT.restrictedApprovalAndAudit`, `ResourceApiIT.bddRestrictedResource`, `RelatorioUtilizacaoIT.relatorioDoDiaComOraculo`, `HistoricoEstadosIT.cicloCompletoGeraUmRegistroPorMudanca`, `CasosDeErroIT.solicitacaoExpiradaNaoPodeSerAprovada` e `AutorizacaoMatrizIT` M16 e M17* | Sim |
| 04 | Somente o Responsável aprova, rejeita e muda o uso | `BookingService.transition`: checagem `if (!responsible) throw ...` removida | 12 testes, entre eles `AutorizacaoMatrizIT` M16 a M20 para Solicitante e Administrador* | Sim |
| 05 | Recurso em manutenção não pode ser reservado | `BookingService.maintenance`: bloqueio gravado com `active=false` | `ResourceApiIT.maintenanceAndReservationsShareExclusion`, `maintenanceCannotOverrideBooking`, `maintenanceEndpointsAndReporting`, `AutorizacaoMatrizIT` M25 (4) | Sim |
| 06 | Autorização: operações exclusivas do Administrador | `ApiController.admin()`: checagem do perfil ADMIN removida | 18 testes, entre eles `AutorizacaoMatrizIT` M06 a M10 para Solicitante e Responsável* | Sim |
| 07 | Posse: só o dono altera a própria reserva | `BookingService.save`: checagem do dono removida | `ResourceApiIT.ownerIsEnforced`, `AutorizacaoMatrizIT.solicitanteNaoMexeNaReservaDeOutro`, `AutorizacaoMatrizIT` M14 para Responsável e Administrador (4) | Sim |
| 08 | Auditoria: toda mudança de estado gera registro de histórico | `BookingService.transition`: chamada `history(...)` removida | `HistoricoEstadosTest.transicaoGravaReservaUsuarioInstanteEstadosEMotivo`, `HistoricoEstadosTest.transicaoSemMotivoGravaMotivoPadrao` (2; a suíte parou nos testes unitários) | Sim |
| 09 | Relatório de utilização não conta solicitação pendente | Consulta do relatório passa a contar `SOLICITADA` | `RelatorioUtilizacaoIT.relatorioDoDiaComOraculo` ("expected 0.0 but was 2.0": o Projetor pendente passou a somar 2 h) (1) | Sim |
| 10 | Término posterior ao início | `Rules.interval`: `!end.isAfter(start)` → `end.isBefore(start)` (aceita início igual ao fim) | `RulesTest.invalidIntervals` (1; a suíte parou nos testes unitários) | Sim |

O número entre parênteses é o total de testes que falharam com a mutação, segundo o resumo do CI. \* O GitHub mostra no máximo 10 anotações de erro por passo: nas mutações 03, 04 e 06 a lista completa está no log do passo "Testes com a regra quebrada" de cada PR, e aqui aparecem só os testes que constam nas anotações. Mutações 08 e 10 pararam na fase de testes unitários; os testes de integração ligados a elas (`HistoricoEstadosIT.cicloCompletoGeraUmRegistroPorMudanca` e a restrição `CHECK(end_at>start_at)` do banco) não chegaram a rodar.

## Achados

- **`Rules.overlaps` não é usado pela aplicação.** O `RulesTest.overlap` testa 7 casos dessa função, mas nenhum código de produção a chama: a proteção real contra sobreposição é a restrição `EXCLUDE` do PostgreSQL (mutações 01 e 02). O teste unitário documenta a regra, mas não protege o sistema. Recomendação: remover a função ou usá-la, e não contar o `RulesTest.overlap` como evidência de RF05 sozinho.
- **A matriz de autorização detecta quebras de perfil em várias rotas ao mesmo tempo.** Nas mutações 04 e 06, cada célula afetada da matriz virou uma falha com a rota e o perfil no nome do teste, o que localiza a quebra direto. Isso reforça a regra do risco RS01: toda rota nova precisa de uma linha na matriz.
- **Nenhuma mutação sobreviveu**, então nenhum teste novo foi necessário. Se uma regra crítica mudar, a mutação correspondente deve ser repetida.

## Como repetir uma mutação

1. Criar uma branch descartável a partir do commit avaliado: `git checkout -b mut/NN-nome`.
2. Aplicar **só** a quebra da tabela acima no arquivo indicado.
3. Rodar `mvn -B verify` (exige Docker) ou abrir um PR de rascunho e ver o CI.
4. Conferir que os testes da coluna "Testes que falharam" ficaram vermelhos.
5. Descartar a mudança: `git restore .` e `git checkout main`, sem commit da mutação na `main`.

Evidência: PRs de rascunho #3 a #12 no fork `lucasrmagalhaess/organizacao-recursos`, fechados sem merge, com o resumo de testes de cada execução nas anotações do job `tests`.
