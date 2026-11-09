# Relatório de qualidade

Atualizado em **05/11/2026** com as execuções do GitHub Actions. Substitui a versão de 24/10/2026, que só tinha uma execução local parcial (Java 17, sem Docker).

## Decisão

**Ainda não liberado.** A suíte automatizada completa roda e passa no CI, e as metas de cobertura foram atingidas com folga. Faltam evidências exigidas para a liberação: análise do SonarCloud com quality gate, resultado do teste de carga e repositório público. A decisão final de liberação será registrada em `docs/decisao-qualidade.md` no commit da tag `v1.0.0-rc1`.

## Resultado em números

| Métrica | Meta | Resultado | Evidência |
| --- | --- | --- | --- |
| Cobertura de linhas | 80% | **95,40%** (332 de 348) | CI "Qualidade #2", `docs/evidencias/ci-qualidade-2/jacoco/` |
| Cobertura de branches | 70% | **84,81%** (134 de 158) | Mesmo relatório JaCoCo |
| Testes automatizados | 100% passando | **159 de 159** | `docs/evidencias/ci-qualidade-2/` |
| Requisitos críticos na RTM | 100% | 100% mapeados; execução no CI registrada na RTM | `RTM.md` |
| Bugs e vulnerabilidades críticas | 0 | **Não medido**: SonarCloud ainda sem `SONAR_TOKEN` | Defeito P03 |

## Execuções

| Execução | Data | Commit | Testes | Linhas | Branches | Job `tests` | Job `e2e` |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CI "Qualidade #2" (PR #2) | 05/11/2026 | 00c121f | 159, 0 falhas | 95,40% | 84,81% | Testes e meta de cobertura passaram; Sonar falhou (sem token) | Falhou: defeito D05 |
| CI do PR #3 (correção D05) | 05/11/2026 | bb48527 | — | — | — | Sonar falhou (sem token) | **Passou** (regressão do D05) |
| CI "Qualidade #1" (push na `main`) | 25/10/2026 | fff753c | 44, 0 falhas | 93,39% | 77,85% | Testes e meta de cobertura passaram; Sonar falhou (sem token) | Passou |
| Local, sem Docker | 23/10/2026 | — | 26 unitários; integração bloqueada | 7,76% | 10,76% | — | — |

Ambiente do CI: GitHub Actions, Java 21, PostgreSQL 16 em Testcontainers e Playwright com Chromium. Os números da execução local de 23/10 eram baixos porque os testes de integração não rodaram; a cobertura real só foi medida no CI. As evidências de cada execução estão em `docs/evidencias/` (índice em `docs/evidencias/resumo.json`).

## Testes por nível

| Nível | Suíte | Testes | Resultado no CI |
| --- | --- | --- | --- |
| Unidade parametrizada | `RulesTest` | 22 | Passou |
| Integração externa isolada (WireMock) | `NotificationGatewayTest` | 4 | Passou |
| Integração com PostgreSQL real | `ResourceApiIT` | 17 | Passou |
| Concorrência (dupla reserva) | `ResourceApiIT.simultaneousRequestsAcceptOnlyOne` | 1 | Passou: uma reserva aceita, uma recusada com 409 |
| API em caixa-preta (HTTP real) | `BlackBoxIT` | 1 | Passou |
| Autorização (matriz 28 operações × 4 perfis, posse, erros) | `AutorizacaoMatrizIT` | 115 | Passou |
| End-to-end (navegador) | `e2e/reservas.spec.js` | 2 | Passou no CI de 25/10 e no PR #3 |
| Relatório de utilização com oráculo | `RelatorioUtilizacaoIT` | 6 | Escrito com TDD; execução no CI do PR do relatório |
| Carga | `load/reservas.jmx` | — | **Não executado** |
| Análise estática | SonarCloud | — | **Não executado** |

## Cobertura por classe (CI "Qualidade #2")

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
| `Application` | 1/3 (33%) | — |

Pontos fracos: `NotificationDelivery` não tem teste do resultado da entrega (ENVIADA ou FALHOU), e `SecurityConfig` tem metade dos branches cobertos. A cobertura alta não substitui a mutação manual, que confirma se os testes detectam a quebra das regras críticas (pendente, Aula 12).

## Defeitos

O registro completo está em `docs/DEFEITOS.md`. Mudanças desta revisão:

- **D05** (CI, alta): o job `e2e` falhou porque `ubuntu-latest` passou a ser Ubuntu 26.04, sem suporte no Playwright 1.55.1. Registrado antes da correção; corrigido fixando `ubuntu-24.04`; **fechado** com o `e2e` verde no PR #3.
- **P01** (ambiente) e **P02** (cobertura): **fechados**, porque o CI executa a suíte completa com Java 21 e Docker e a cobertura passou da meta.
- **D01, D02 e D03**: **fechados** por execução dos testes de regressão no CI.
- **D04**: ainda não tinha teste de regressão. Foi adicionado `ResourceApiIT.regressaoD04RecursoComReservaFuturaNaoPodeSerEditado`; fecha quando passar no CI.

## Pendências para a liberação

| Pendência | Responsável | Impacto se ficar de fora |
| --- | --- | --- |
| Configurar `SONAR_TOKEN`, `SONAR_PROJECT_KEY` e `SONAR_ORGANIZATION` | Dono do repositório | CI sempre vermelho; meta "0 bugs críticos" sem evidência |
| Tornar o repositório público | Dono do repositório | Professor não consegue clonar: limitador "aplicação não executa" |
| Executar o JMeter e registrar `docs/performance/resultado-01.md` | Equipe | Sem evidência de desempenho (RNF03) |
| Auditoria estruturada (estado anterior e novo, motivo, `Clock`, rollback) | Auditoria | RF10 incompleto em relação à Aula 11 |
| Mutação manual (`docs/testes/mutacao-manual.md`) | Auditoria | RTM sem prova de que os testes detectam a quebra das regras |
| Revisão por pares em todos os PRs | Equipe | Critério de autoria e revisão |

## Como reproduzir

```sh
mvn -B verify                     # unidade, integração e cobertura (exige Docker)
mvn -B jacoco:check@coverage-gate -Pquality
docker compose up -d --build && npm install && npx playwright install --with-deps chromium && npm run test:e2e
```

A aplicação e o CI usam Java 21. Os relatórios ficam em `target/surefire-reports/`, `target/failsafe-reports/` e `target/site/jacoco/`; no CI, no artifact `testes-e-cobertura` de cada execução.
