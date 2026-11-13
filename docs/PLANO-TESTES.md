# Plano de testes

## Objetivo e ambiente

Demonstrar correção funcional, autorização, integridade concorrente e rastreabilidade. Ambiente alvo: Java 21, Maven 3.9+, PostgreSQL 16 em Testcontainers, WireMock 3.13.1, Playwright/Chromium e JMeter 5.6.3. CI: Ubuntu GitHub Actions com Docker. Banco de teste descartável, sem dados pessoais reais.

## Níveis

| Nível | Local | Critério |
|---|---|---|
| Unidade parametrizada | RulesTest | Fronteiras de intervalo, intervalos inválidos e transições |
| Integração externa isolada | NotificationGatewayTest | POST exato, 2xx/503, não seguir redirect, URL inválida |
| Integração PostgreSQL | ResourceApiIT | Persistência real, rollback, autorização, auditoria, manutenção e material |
| Concorrência HTTP | simultaneousRequestsAcceptOnlyOne | Barreira entre threads, exatamente 1 sucesso + 1 conflito e 1 reserva persistida |
| API caixa-preta | BlackBoxIT | HTTP real, cookie, login, CSRF renovado, cadastro e consulta |
| End-to-end | e2e/reservas.spec.js | Login, cadastro, reserva e layout móvel no navegador real |
| Carga | load/reservas.jmx | Três rodadas no CI (10, 50 e 100 usuários sincronizados): em cada uma, 1 sucesso e os demais 409 |
| Estática | SonarCloud | Nenhum bug/vulnerabilidade crítica e quality gate |

Mocks de banco não são usados. O Testcontainers não está configurado para ignorar testes sem Docker: ausência de Docker deve falhar explicitamente em `verify`.

## Partições e fronteiras

- [10h,11h) x [11h,12h): permitido. Mesmos horários, contenção e interseção parcial: rejeitados.
- Início igual ao fim ou depois do fim: rejeitado. Início passado: rejeitado na criação/alteração.
- Recurso livre, restrito, inativo, inexistente ou em manutenção.
- Solicitante dono, solicitante diferente, responsável, administrador e anônimo.
- Transições permitidas, terminal para ativo, início antecipado, conclusão com devolução pendente.
- Edição conflitante deve preservar a versão anterior e suas alocações.
- Cancelamento futuro libera agenda sem apagar histórico.

## BDD

Especificação em `bdd/aprovacao.feature`, automatizada em `ResourceApiIT.bddRestrictedResource` com DisplayName em linguagem de negócio. O arquivo `.feature` é documentação, não executa com Cucumber. A equipe deve revisar o cenário antes de modificar a funcionalidade e guardar PR, discussão e resultado. Não há alegação de TDD retrospectivo nem commits red/green inventados.

## Execução e evidências

1. `./mvnw clean verify`: guardar surefire, failsafe e JaCoCo XML/HTML.
2. `./mvnw verify -Pquality`: impor limites. Se falhar, localizar regras não cobertas e adicionar testes significativos.
3. Subir aplicação, definir ADMIN_PASSWORD, executar Playwright e guardar HTML/trace.
4. Executar carga conforme PLANO-CARGA.md, guardar JTL e resumo.
5. Rodar SonarCloud e anexar link permanente com commit.
6. Abrir defeito para cada falha; adicionar regressão e repetir somente testes relevantes, depois suíte completa para release.

## Critérios de entrada/saída

Entrada: banco disponível, migrações aplicadas, contas e fixtures isoladas, ambiente identificado. Saída: requisitos críticos aprovados, cobertura mínima real, qualidade estática sem críticos, exatamente 1 reserva na concorrência, relatório de carga com hardware e resultado, defeitos graves fechados e revisão por colega.

Falha de autorização, dupla reserva, aplicação sem execução ou pipeline ausente bloqueiam a liberação. A equipe deve completar nomes, datas e URLs das evidências reais.
