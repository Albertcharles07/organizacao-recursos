# Relatório de qualidade

Atualizado em **10/11/2026** com as execuções do GitHub Actions. Substitui a versão de 24/10/2026, que só tinha uma execução local parcial (Java 17, sem Docker).

## Decisão

**Ainda não liberado.** A suíte completa passa no CI, a cobertura passou da meta com folga, o teste de carga foi aprovado, as 10 mutações das regras críticas foram detectadas e o quality gate do SonarCloud está aprovado. Os PRs da fila foram integrados na `main` em 10/11/2026, e o CI da `main` passou. Falta confirmar a nota de segurança A depois da correção do D09 e repetir o CI completo no commit da tag. A decisão final será registrada em `docs/decisao-qualidade.md` no commit da tag `v1.0.0-rc1`.

## Resultado em números

| Métrica | Meta | Resultado | Evidência |
| --- | --- | --- | --- |
| Cobertura de linhas | 80% | **97,63%** (371 de 380) | CI da `main`, commit `7a037b3` |
| Cobertura de branches | 70% | **92,61%** (163 de 176) | CI da `main`, commit `7a037b3` |
| Testes automatizados | 100% passando | **211 de 211** | CI da `main`, commit `7a037b3` |
| Mutações das regras críticas detectadas | 100% | **10 de 10** | `docs/testes/mutacao-manual.md` |
| Dupla reserva sob carga | 1 reserva por rodada | **1 em 1** com 10, 50 e 100 usuários, em duas execuções; p95 até 487 ms | `docs/performance/resultado-01.md` |
| Requisitos críticos na RTM com evidência | 100% | **100%** | `docs/rtm.md` |
| Bugs e vulnerabilidades críticas | 0 | **0**; quality gate aprovado. 2 issues de segurança médias (D09), corrigidas | SonarCloud, `main`, 06/11/2026 |
| Cobertura e duplicação no SonarCloud | — | 92,1% e 0,0% | SonarCloud, `main`, 06/11/2026 |

## Execuções

| Execução | Data | Commit | Testes | Linhas | Branches | Observação |
| --- | --- | --- | --- | --- | --- | --- |
| CI do grupo, push na `main` ("Qualidade #21") | 10/11/2026 | `7a037b3` | 211, 0 falhas | 97,63% | 92,61% | **Referência atual**: `main` com os PRs #4 a #9; `tests`, `e2e` e `load` aprovados |
| CI do grupo, PR #4 ("Qualidade #16") | 10/11/2026 | `52fbd3d` | — | — | — | `e2e` falhou: secret `ADMIN_PASSWORD` curto (defeito D10), corrigido |
| Validação no fork (PR #2 do fork) | 05/11/2026 | `2801221` | 208, 0 falhas | 97,65% | 92,53% | Validação com todos os PRs da fila; E2E e carga aprovados |
| Validação no fork | 05/11/2026 | `be6fda0` | 208, 0 falhas | 97,65% | 92,53% | Job de carga falhou ao baixar o JMeter (defeito D08), corrigido |
| Validação no fork, com carga | 05/11/2026 | `0170b7b` | 202, 0 falhas | 97,65% | 92,53% | Primeira execução do job `load`: aprovado |
| Validação no fork | 05/11/2026 | `6649bdc` | 187, 1 falha | — | — | Falha no próprio teste de rollback (defeito D07), corrigida |
| CI do grupo, PR #3 (D05) | 05/11/2026 | `bb48527` | — | — | — | E2E aprovado: regressão do D05 |
| CI do grupo, PR #2 (matriz) | 05/11/2026 | `00c121f` | 159, 0 falhas | 95,40% | 84,81% | E2E falhou: defeito D05 |
| CI do grupo, push na `main` | 25/10/2026 | `fff753c` | 44, 0 falhas | 93,39% | 77,85% | Primeira execução do CI |
| Local, sem Docker | 23/10/2026 | — | 26 unitários | 7,76% | 10,76% | Integração bloqueada |

Até 05/11/2026, o job `tests` terminava vermelho só no passo do SonarCloud, por falta do token. Desde 06/11/2026, com o SonarCloud configurado, os três checks do PR #4 do grupo (`tests`, `e2e` e SonarCloud) passam. O passo "Resumo dos testes e da cobertura" publica o total e cada teste que falhar como anotação no PR. Evidências: `docs/evidencias/README.md`.

## Testes por nível

| Nível | Suíte | Testes |
| --- | --- | --- |
| Unidade parametrizada | `RulesTest` | 22 |
| Unidade com Clock fixo e dublê do banco | `HistoricoEstadosTest` | 6 |
| Integração externa isolada (WireMock) | `NotificationGatewayTest` | 4 |
| Integração com PostgreSQL real | `ResourceApiIT` | 18 |
| Concorrência (dupla reserva) | `ResourceApiIT.simultaneousRequestsAcceptOnlyOne` | (incluído acima) |
| API em caixa-preta (HTTP real) | `BlackBoxIT` | 1 |
| Autorização (30 operações × 4 perfis, posse, erros) | `AutorizacaoMatrizIT` | 123 |
| Relatório de utilização com oráculo | `RelatorioUtilizacaoIT` | 6 |
| Histórico auditável, rollback e imutabilidade | `HistoricoEstadosIT` | 7 |
| Entrega da notificação (WireMock + PostgreSQL) | `NotificacaoEntregaIT` | 4 |
| Caminhos de erro e transições | `CasosDeErroIT` | 11 |
| Filtros de pesquisa | `FiltrosRecursosIT` | 6 |
| End-to-end (navegador) | `e2e/reservas.spec.js` | 2 |
| Carga | `load/reservas.jmx` (job `load`) | 3 rodadas |
| Análise estática | SonarCloud | Quality gate aprovado |

TDD: o relatório de utilização e o histórico de estados foram escritos com commits separados de teste ("red") e de implementação ("green"). Os prints do CI vermelho e verde de cada um estão em `docs/evidencias/tdd/`. BDD: `docs/bdd/aprovacao.feature`, automatizado em `ResourceApiIT.bddRestrictedResource`.

## Cobertura por classe (CI do commit `00c121f`)

| Classe | Linhas | Branches |
| --- | --- | --- |
| `Rules` | 11/11 (100%) | 14/14 (100%) |
| `ActiveAccountFilter` | 15/15 (100%) | 7/10 (70%) |
| `ApiController` | 115/117 (98%) | 37/42 (88%) |
| `BookingService` | 114/118 (97%) | 62/68 (91%) |
| `SecurityConfig` | 35/36 (97%) | 3/6 (50%) |
| `ApiErrors` | 17/18 (94%) | 7/10 (70%) |
| `NotificationGateway` | 16/19 (84%) | 3/4 (75%) |
| `NotificationDelivery` | 8/11 (73%) | 1/4 (25%) |

Tabela da última execução com o relatório por classe disponível. Depois dela, `NotificacaoEntregaIT` passou a cobrir os caminhos ENVIADA e FALHOU do `NotificationDelivery`, e a cobertura global subiu para 97,65% e 92,53%. Cobertura alta não substitui a mutação manual, que confirmou que os testes detectam a quebra das regras críticas.

## SonarCloud

Análise da `main` em 06/11/2026 (736 linhas de código): **quality gate aprovado**, cobertura de 92,1% e duplicação de 0,0%. Foram apontadas 2 issues de segurança de severidade média, "Make sure using a dynamically formatted SQL query is safe here", em `GET /resources` e `GET /bookings`, que deixaram a nota de segurança em C. A análise da auditoria concluiu que não são exploráveis (os trechos concatenados são constantes do código e a entrada do usuário vai por parâmetro), mas o padrão foi corrigido para não virar brecha numa alteração futura (defeito D09).

Análise do PR #4 (relatório de utilização): quality gate aprovado, 0 issues novas, 0 security hotspots, 100% de cobertura e 0,0% de duplicação no código novo.

A cobertura do SonarCloud (92,1%) é menor que a do JaCoCo no CI (97,65%) porque as duas ferramentas contam linhas de modo diferente e o SonarCloud mediu a `main` antes dos PRs da fila.

## Defeitos

Registro completo em `docs/defeitos/README.md`. Fechados nesta revisão, cada um com a execução que comprova: D01 a D05, D07, D08, P01 e P02. Fechados também o **P03**, com o SonarCloud no CI, o **P04**, com os PRs #4 a #9 aprovados e integrados em 10/11/2026, e o **D10** (secret `ADMIN_PASSWORD` curto no CI). Corrigido e aguardando a análise do SonarCloud: **D09** (SQL montado por concatenação). Abertos: **P05** (webhook sem nova tentativa, limitação aceita) e **D06** (`Rules.overlaps` não é usado pela aplicação, baixa prioridade).

## Pendências para a liberação

| Pendência | Responsável | Impacto se ficar de fora |
| --- | --- | --- |
| Repetir o CI completo no commit da tag | Equipe | A evidência final precisa ser do commit entregue |
| Checklist de clone limpo | Integrante com Docker | Bloqueadores do README só aparecem num clone do zero |

## Como reproduzir

```sh
./mvnw -B verify                                # unidade, integração e cobertura (exige Docker)
./mvnw -B jacoco:check@coverage-gate -Pquality  # metas 80/70
docker compose up -d --build && npm install && npx playwright install --with-deps chromium && npm run test:e2e
```

A carga roda no job `load` do CI (ver `docs/PLANO-CARGA.md` para rodar localmente). Os relatórios ficam em `target/surefire-reports/`, `target/failsafe-reports/` e `target/site/jacoco/`; no CI, nos artifacts `testes-e-cobertura`, `e2e` e `carga-jmeter`.
