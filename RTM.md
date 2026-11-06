# RTM — requisito, risco, teste e evidência

Status de rastreabilidade: todos os 14 requisitos funcionais e as regras críticas abaixo têm ligação proposta. **Mapeado não significa aprovado.** Evidências de execução nesta entrega: `docs/evidencias/`. Testes que dependem de Docker, navegador e JMeter ainda precisam de execução no ambiente da equipe.

| ID | Requisito | Risco | Implementação | Teste/verificação | Evidência / estado |
|---|---|---|---|---|---|
| RF01* | Autenticação e autorização | Acesso indevido | SecurityConfig, ActiveAccountFilter, API | AutorizacaoMatrizIT (matriz em docs/seguranca/matriz-autorizacao.md: 28 operações × 4 perfis, posse); BlackBoxIT; inputsAndPermissions; ownerIsEnforced; bddRestrictedResource | Matriz e teste escritos; execução no CI pendente |
| RF02 | Cadastros e consulta | Cadastro inconsistente | /resources e /users | adminResourceAndUserFlows; E2E | Execução IT/E2E pendente |
| RF03 | Filtros e disponibilidade | Disponibilidade falsa | GET /resources | availabilityFiltersAndPrivacy | Execução IT pendente |
| RF04 | Criar, alterar e cancelar | Perda de dados | BookingService.save/transition | cancelReleasesResources; failedEditRollsBack; ownerIsEnforced | Execução IT pendente |
| RF05* | Sobreposição sala/material/professor | Recurso duplicado | allocation EXCLUDE GiST | RulesTest.overlap; teacherCannotBeInTwoRooms; adjacentReservationsAreAccepted | Unitários executados; IT pendente |
| RF06* | Concorrência | Dupla reserva simultânea | Transação + exclusão | simultaneousRequestsAcceptOnlyOne; JMeter | IT/carga pendentes |
| RF07* | Aprovação restrita | Uso sem autorização | estado inicial e perfil | restrictedApprovalAndAudit; bddRestrictedResource | Cenário BDD especificado; IT pendente |
| RF08* | Bloqueio/manutenção | Reserva indisponível | allocation sem booking_id | maintenanceAndReservationsShareExclusion; maintenanceCannotOverrideBooking | IT pendente |
| RF09 | Retirada/devolução | Material não devolvido | material_movement | startedBookingAndMaterialLifecycle | IT pendente |
| RF10* | Histórico auditável | Alteração sem autoria | audit + trigger | restrictedApprovalAndAudit; auditCannotBeRewritten | IT pendente |
| RF11 | Notificação | Falha externa compromete fluxo | notification + gateway | NotificationGatewayTest; restrictedApprovalAndAudit | WireMock executado; IT pendente |
| RF12 | Utilização, horas, conflitos | Indicadores incorretos | GET /reports | maintenanceEndpointsAndReporting; simultaneousRequestsAcceptOnlyOne | IT pendente; conferir valores na demo |
| RF13 | Responsividade e erros | Operação incompreensível | static + ApiErrors | E2E móvel/desktop; inputsAndPermissions; AutorizacaoMatrizIT.entradasInvalidasRetornam400SemDetalhesInternos | Sintaxe JS verificada; E2E e IT pendentes |
| RF14 | Documentação | Uso sem contrato | docs/API.md, README | Revisão por colega e BlackBoxIT | Documento entregue; revisão humana pendente |
| RC01* | Fim > início | Intervalo inválido | Rules.interval + CHECK SQL | RulesTest.invalidIntervals | Unitário executado |
| RC02* | Reserva iniciada não apagada | Perda de histórico | Sem DELETE; restrições de estado | startedBookingAndMaterialLifecycle; inspeção de rotas | IT pendente |
| RC03* | Toda transição auditada | Rastreio incompleto | Auditoria mesma transação | restrictedApprovalAndAudit | IT pendente |
| RNF01 | 80% linhas / 70% branches | Testes insuficientes | JaCoCo + quality profile | mvn verify -Pquality | Não demonstrado pela execução parcial |
| RNF02 | Zero problemas críticos | Vulnerabilidade/bug crítico | SonarCloud quality gate | Scanner Maven no Actions | Conta/token e análise pendentes |
| RNF03 | Carga e robustez | Latência/falha sob concorrência | JMeter e timeout webhook | load/reservas.jmx; summarize.py; WireMock | WireMock executado; carga pendente |

`*` = requisito crítico para o aceite. Cobertura de rastreabilidade crítica: 100% mapeados nesta matriz; cobertura de execução crítica: parcial. Atualizar cada linha com URL da execução, commit e artifact, sem substituir uma pendência por “aprovado” apenas porque existe um teste.
