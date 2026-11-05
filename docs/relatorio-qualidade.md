# Relatório de qualidade

Atualizado em **05/11/2026** com as execuções do GitHub Actions. Substitui a versão de 24/10/2026, que só tinha uma execução local parcial (Java 17, sem Docker).

## Decisão

**Ainda não liberado.** A suíte completa (208 testes) passa no CI, a cobertura passou da meta com folga, o teste de carga foi aprovado e as 10 mutações das regras críticas foram detectadas. Faltam duas evidências exigidas: a análise do SonarCloud com quality gate e a execução final no repositório do grupo, que também precisa ficar público. A decisão final será registrada em `docs/decisao-qualidade.md` no commit da tag `v1.0.0-rc1`.

## Resultado em números

| Métrica | Meta | Resultado | Evidência |
| --- | --- | --- | --- |
| Cobertura de linhas | 80% | **97,65%** (374 de 383) | CI do commit `2801221` |
| Cobertura de branches | 70% | **92,53%** (161 de 174) | CI do commit `2801221` |
| Testes automatizados | 100% passando | **208 de 208** | CI do commit `2801221` |
| Mutações das regras críticas detectadas | 100% | **10 de 10** | `docs/testes/mutacao-manual.md` |
| Dupla reserva sob carga | 1 reserva por rodada | **1 em 1** com 10, 50 e 100 usuários, em duas execuções; p95 até 487 ms | `docs/performance/resultado-01.md` |
| Requisitos críticos na RTM com evidência | 100% | **100%** | `docs/rtm.md` |
| Bugs e vulnerabilidades críticas | 0 | **Não medido**: SonarCloud sem `SONAR_TOKEN` | Defeito P03 |

## Execuções

| Execução | Data | Commit | Testes | Linhas | Branches | Observação |
| --- | --- | --- | --- | --- | --- | --- |
| Validação no fork (PR #2 do fork) | 05/11/2026 | `2801221` | 208, 0 falhas | 97,65% | 92,53% | **Referência atual**: inclui todos os PRs da fila; E2E e carga aprovados |
| Validação no fork | 05/11/2026 | `be6fda0` | 208, 0 falhas | 97,65% | 92,53% | Job de carga falhou ao baixar o JMeter (defeito D08), corrigido |
| Validação no fork, com carga | 05/11/2026 | `0170b7b` | 202, 0 falhas | 97,65% | 92,53% | Primeira execução do job `load`: aprovado |
| Validação no fork | 05/11/2026 | `6649bdc` | 187, 1 falha | — | — | Falha no próprio teste de rollback (defeito D07), corrigida |
| CI do grupo, PR #3 (D05) | 05/11/2026 | `bb48527` | — | — | — | E2E aprovado: regressão do D05 |
| CI do grupo, PR #2 (matriz) | 05/11/2026 | `00c121f` | 159, 0 falhas | 95,40% | 84,81% | E2E falhou: defeito D05 |
| CI do grupo, push na `main` | 25/10/2026 | `fff753c` | 44, 0 falhas | 93,39% | 77,85% | Primeira execução do CI |
| Local, sem Docker | 23/10/2026 | — | 26 unitários | 7,76% | 10,76% | Integração bloqueada |

Em todas as execuções do CI, o job `tests` termina vermelho só no passo do SonarCloud, por falta do token; o passo de testes e o da meta de cobertura passam. O passo "Resumo dos testes e da cobertura" publica o total e cada teste que falhar como anotação no PR. Evidências: `docs/evidencias/README.md`.

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
| Análise estática | SonarCloud | **Pendente** |

TDD: o relatório de utilização e o histórico de estados foram escritos com commits separados de teste ("red") e de implementação ("green"). BDD: `docs/bdd/aprovacao.feature`, automatizado em `ResourceApiIT.bddRestrictedResource`.

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

## Defeitos

Registro completo em `docs/defeitos/README.md`. Fechados nesta revisão, cada um com a execução que comprova: D01 a D05, D07, D08, P01 e P02. Abertos: **P03** (SonarCloud sem token), **P04** (revisão por pares em andamento), **P05** (webhook sem nova tentativa, limitação aceita) e **D06** (`Rules.overlaps` não é usado pela aplicação, baixa prioridade).

## Pendências para a liberação

| Pendência | Responsável | Impacto se ficar de fora |
| --- | --- | --- |
| Tornar o repositório público | Dono do repositório | Professor não consegue clonar: limitador "aplicação não executa" |
| Configurar `SONAR_TOKEN`, `SONAR_PROJECT_KEY` e `SONAR_ORGANIZATION`; ler o quality gate | Dono do repositório; auditoria lê o resultado | CI sempre vermelho; meta "0 críticos" sem evidência |
| Revisão e merge dos PRs da fila | Integrantes | Critério de revisão por pares; o trabalho fica fora da `main` |
| Repetir o CI completo no repositório do grupo, no commit da tag | Equipe | Evidências atuais são do fork de validação |
| Checklist de clone limpo | Integrante com Docker | Bloqueadores do README só aparecem num clone do zero |

## Como reproduzir

```sh
mvn -B verify                                   # unidade, integração e cobertura (exige Docker)
mvn -B jacoco:check@coverage-gate -Pquality     # metas 80/70
docker compose up -d --build && npm install && npx playwright install --with-deps chromium && npm run test:e2e
```

A carga roda no job `load` do CI (ver `docs/PLANO-CARGA.md` para rodar localmente). Os relatórios ficam em `target/surefire-reports/`, `target/failsafe-reports/` e `target/site/jacoco/`; no CI, nos artifacts `testes-e-cobertura`, `e2e` e `carga-jmeter`.
