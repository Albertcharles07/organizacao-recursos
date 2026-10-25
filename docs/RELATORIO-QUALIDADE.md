# Relatório de qualidade — entrega inicial

Relatório preparado em **24/10/2026**. Execução disponível: **23/10/2026**. Projeto-alvo: Java 21, Spring Boot 3.5.16, PostgreSQL 16. A compilação local usou Java 17.0.20 por indisponibilidade de Java 21 no ambiente, com override explícito de `java.version`. O `pom.xml`, Dockerfile e CI permanecem configurados para Java 21.

## Decisão

**NÃO HOMOLOGADO para a entrega final.** Código, documentação, planos e suítes estão fornecidos, mas falta demonstrar critérios críticos no ambiente alvo. Este relatório não declara a aplicação inteira validada, ausência de vulnerabilidades ou cobertura mínima atingida.

## Evidências reais

| Verificação | Resultado | Evidência |
|---|---|---|
| Compilação do código principal e dos testes com Java 17 | Concluída na execução local | Relatórios de execução e classes compiladas durante o trabalho |
| JUnit parametrizado e regras | 22 testes, 0 falhas, 0 erros | `evidencias/surefire-reports/br.edu.recursos.RulesTest.txt` |
| Cliente HTTP com WireMock | 4 testes, 0 falhas, 0 erros | `evidencias/surefire-reports/br.edu.recursos.NotificationGatewayTest.txt` |
| Testcontainers e API HTTP real | Bloqueados antes dos cenários por ausência de Docker | `evidencias/failsafe-reports/` (2 erros de inicialização) |
| Cobertura de linhas global na execução parcial | **7.76%** | `evidencias/jacoco-unitarios/index.html` |
| Cobertura de branches global na execução parcial | **10.76%** | Mesmo relatório JaCoCo |
| Sintaxe JavaScript, JSON, XML Maven/JMeter e YAML | Verificada localmente | Repetir comandos abaixo para verificação reproduzível |
| Slides | 9 slides renderizados e revisados, pacote validado | `Apresentacao-Recursos.pptx` |
| Playwright | Não executado: navegador não ficou disponível | Suíte em `e2e/`, execução pendente |
| JMeter | Não executado | Plano e verificador em `load/` |
| SonarCloud | Não executado | Exige organização, projeto e secret da equipe |
| GitHub Actions | YAML criado, execução remota pendente | `.github/workflows/ci.yml` |

Os percentuais acima medem o **projeto inteiro com apenas a suíte parcial executada**. São inferiores às metas de 80%/70%; não representam a cobertura que a suíte completa produzirá. É necessário executar os testes com banco real e, se ainda ficar abaixo da meta, ampliar a suíte com casos relevantes. Não há justificativa para retirar classes da cobertura apenas para tornar o resultado verde.

## Comandos usados e próximos comandos

Na execução local, com Maven 3.9.9:

```sh
mvn -B -Djava.version=17 clean test jacoco:report
mvn -B -Djava.version=17 failsafe:integration-test failsafe:verify
node --check src/main/resources/static/app.js
node --check e2e/reservas.spec.js
```

O primeiro comando completou com 26 testes aprovados. O segundo falhou por falta de Docker. A configuração de rede do ambiente foi ajustada somente no Maven local, fora do projeto. Os logs temporários completos não permaneceram após a retomada da sessão; os relatórios originais Surefire/Failsafe e JaCoCo foram preservados e estão anexados.

No computador da equipe/CI, **com Java 21 e Docker**, rodar:

```sh
mvn clean verify -Pquality
npm install
npx playwright install chromium
npm run test:e2e
```

Para Playwright, subir a aplicação antes e definir ADMIN_PASSWORD. Para JMeter, seguir `PLANO-CARGA.md`. Registrar commit, ambiente e link dos artifacts. O pipeline não deve ser considerado aprovado apenas por existir o arquivo YAML.

## Rastreabilidade e processo

Os 14 requisitos funcionais e as regras críticas têm vínculos na RTM. Isso comprova o mapeamento documental, não o comportamento em execução. O cenário BDD e o teste correspondente estão entregues; a discussão/revisão da equipe e sua evidência em PR ainda são necessárias. Não foi produzido histórico fictício de TDD, revisão ou contribuições de integrantes.

## Pendências para liberação

1. Executar aplicação e suíte completa com Java 21/PostgreSQL.
2. Demonstrar autorização e concorrência sem falhas.
3. Atingir e comprovar metas reais de cobertura.
4. Executar navegador, carga e SonarCloud.
5. Publicar repositório, executar CI em PR e registrar revisão por pares.
6. Atualizar RTM, defeitos e apresentação com resultados reais.

As limitações de negócio e operacionais estão em `REGRAS.md` e `ARQUITETURA.md`. A decisão de liberação deve ser refeita depois dessas verificações.
