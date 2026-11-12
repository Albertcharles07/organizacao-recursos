# Checklist da entrega (clone limpo)

Roteiro da Aula 14, etapa 1. **Quem verifica precisa clonar o repositório do zero, numa pasta nova, e seguir só o README**, sem reaproveitar nada da máquina de quem desenvolveu. Nesta etapa só se anota; nada é corrigido.

- Data da verificação: 12/11/2026
- Quem verificou: Carlos Daniel de Souza Gomes
- Commit verificado: f4a706f9de7b5547704cdb735dbcf156b0757bb9
- Sistema operacional e versão do Docker: Windows; Docker 29.9.2, build a82e528
- Java utilizado na verificação Maven: 21.0.13

## Artefatos

A coluna "Existe e caminho" foi preenchida a partir do repositório em 05/11/2026. Os resultados abaixo registram as verificações locais e a revisão documental realizada via GitHub. “Não conferido localmente” não significa reprovação.

As observações sobre atualização descrevem a situação encontrada no commit avaliado. Textos preparados posteriormente só serão considerados incorporados após serem salvos e conferidos no repositório.

| Item exigido | Existe e caminho | Atualizado? | Conferido no clone? |
| --- | --- | --- | --- |
| README com instalação reproduzível | `README.md` | Sim — instruções de inicialização validadas | Sim |
| Aplicação sobe só com o README (`docker compose up --build -d`) | `compose.yaml`, `Dockerfile`, `.env.example` | Sim — inicialização validada | Sim |
| `./mvnw -B verify` passa | `pom.xml`, `mvnw`, `.mvn/wrapper/` | Sim — execução concluída com sucesso | Sim |
| Documentação da API | `docs/API.md` | Conteúdo revisado via GitHub; correspondência completa com o código não validada | Não conferido localmente |
| RTM | `docs/rtm.md` | Parcial — atualizar referências para o commit final entregue | Não conferido localmente |
| Diagramas de contexto, componentes e sequências críticas | `docs/ARQUITETURA.md` | Diagramas presentes; correspondência completa com o código não validada | Não conferido localmente |
| Plano de testes | `docs/PLANO-TESTES.md` | Parcial — corrigir caminho do BDD e alinhar descrição das rodadas de carga | Não conferido localmente |
| Relatório de testes e de qualidade | `docs/relatorio-qualidade.md` | Parcial — registrar o clone limpo e atualizar a referência do CI | Não conferido localmente |
| Pipeline de CI | `.github/workflows/ci.yml` | Arquivo revisado via GitHub; testes, cobertura, SonarCloud, E2E e carga aprovados na execução Qualidade #23 | Não conferido localmente |
| Relatórios do JaCoCo | Local: `target/site/jacoco/index.html`; CI: artifact `testes-e-cobertura` | Sim — resultados vinculados ao commit verificado | Sim — relatório local e artifact do CI conferidos |
| Relatório do SonarCloud | Passo do SonarCloud no CI | Quality gate aprovado na execução Qualidade #23; painel detalhado não revisado | Não se aplica ao clone — verificação via CI |
| Plano e resultado da carga | `docs/PLANO-CARGA.md`, `docs/performance/resultado-01.md` | Parcial — corrigir contradição sobre ausência de resultados e atualizar referências; job de carga aprovado na execução #23 | Não conferido localmente |
| Registro de defeitos | `docs/defeitos/README.md` | Revisado via GitHub — D06 permanece aberto e P05 é limitação documentada | Não conferido localmente |
| Matriz de autorização | `docs/seguranca/matriz-autorizacao.md` | Parcial — contém 30 operações; DV01 e OB02 ainda exigem decisão da equipe | Não conferido localmente |
| Mutação manual | `docs/testes/mutacao-manual.md` | Evidência histórica de 10/10 mutações detectadas; não repetida no commit verificado | Não conferido localmente |
| Status dos requisitos | `docs/status-requisitos.md` | Parcial — registrar o clone limpo e atualizar referências finais | Não conferido localmente |
| Índice de evidências | `docs/evidencias/README.md` | Texto atualizado preparado com links do CI #23; incorporação ao repositório não confirmada | Não conferido localmente |
| Decisão de qualidade | `docs/decisao-qualidade.md` | Proposta técnica e critérios preparados; incorporação não confirmada; decisão final, tag e responsáveis pendentes | Não conferido localmente |
| `CHANGELOG.md` | `CHANGELOG.md` | Parcial — conferir e atualizar a indicação da versão candidata “a criar” | Não conferido localmente |

Observação para a verificação: o projeto usa o Maven Wrapper; não é preciso ter o Maven instalado, só o JDK 21.

A revisão documental foi realizada via GitHub, com apoio do ChatGPT, nos arquivos do commit f4a706f9de7b5547704cdb735dbcf156b0757bb9. Essa análise não representa conferência local de todos os documentos nem reexecução dos testes históricos de carga e mutação.

## Resultado da execução

### Clone e inicialização

O repositório foi clonado em uma pasta nova chamada `teste-limpo`. A aplicação iniciou normalmente seguindo as instruções da seção “Começar pelo Docker” do README, com o comando `docker compose up --build -d`.

Foi possível acessar http://localhost:8080 e entrar com o usuário admin. Não ocorreram erros durante esses passos. As senhas do `.env` não foram incluídas neste registro.

### Testes automatizados

O comando de verificação do Maven foi executado com Java 21.0.13 e terminou com BUILD SUCCESS.

- Testes unitários: 32.
- Testes de integração: 179.
- Total: 211 testes.
- Falhas: 0.
- Erros: 0.
- Testes ignorados: 0.
- Tempo total: 4min31s.
- Término: 12/11/2026 às 16h29min29s (UTC−03:00).

### Cobertura JaCoCo

O relatório foi gerado e visualizado localmente em `target/site/jacoco/index.html`. A existência do arquivo também foi confirmada com `Test-Path`, que retornou `True`.

Resultados do relatório local:

- Cobertura de instruções: 98%.
- Cobertura de ramificações: 92%, conforme exibição do HTML.
- Linhas cobertas: 371 de 380, aproximadamente 97,63%.
- Classes analisadas: 17.

### Evidência do GitHub Actions

Foi identificada a execução Qualidade #23, concluída com sucesso e referente ao mesmo commit verificado:

https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609

Nessa execução, os jobs `tests`, `e2e` e `load` foram aprovados. Também passaram as etapas de metas de cobertura e “SonarCloud e quality gate”.

O relatório no artifact `testes-e-cobertura` foi conferido. O painel detalhado do SonarCloud e o conteúdo do artifact `carga-jmeter` não foram revisados nesta verificação.

Artifacts vinculados à execução:

- Testes e cobertura: https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13811152261
- E2E: https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13812235058
- Carga: https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13811003153

### Preparação do fechamento documental

Foram preparados:

- Uma proposta técnica de decisão de qualidade, com critérios, evidências, riscos e condições para o fechamento.
- Um índice de evidências vinculado ao commit avaliado e à execução Qualidade #23.
- A distinção entre resultados locais, resultados do CI e evidências históricas de carga e mutação.

A proposta técnica é liberar com ressalvas após conferir o commit final, criar a tag e obter a confirmação da equipe. Isso não representa aprovação coletiva nem publicação da versão.

A tentativa de salvar pelo acesso integrado ao GitHub retornou erro 403, sem alterar os arquivos. A incorporação dos textos ao repositório precisa ser confirmada.

### Pendências documentais

- Salvar e conferir os textos preparados da decisão de qualidade e do índice de evidências, preservando as declarações dos integrantes.
- Atualizar RTM, relatório de qualidade e status dos requisitos com o clone limpo e as referências finais.
- Corrigir no plano de testes o caminho `bdd/aprovacao.feature` para `docs/bdd/aprovacao.feature` e alinhar a descrição da carga às rodadas de 10, 50 e 100 usuários.
- Corrigir no plano de carga a frase “Sem resultado medido nesta entrega”, que contradiz os resultados apresentados no próprio documento.
- Manter os resultados antigos da carga identificados pelos commits em que foram medidos.
- Registrar a decisão da equipe sobre DV01 e confirmar OB02 na matriz de autorização.
- Definir os responsáveis e confirmar a aceitação dos riscos D06, P05 e das limitações da medição de carga.
- Definir o commit final da entrega e conferir seu CI completo.
- Criar a tag anotada `v1.0.0-rc1` e registrar seu SHA e a execução correspondente.
- Confirmar a decisão final da equipe e atualizar o CHANGELOG.

Na consulta realizada em 12/11/2026, a tag `v1.0.0-rc1` não foi encontrada.

### Limites da verificação

A revisão documental identificou conteúdo e pendências, mas não validou integralmente a correspondência de toda a documentação com o código.

Não foram registrados testes manuais de cadastro, reservas ou permissões dos perfis RESPONSAVEL e SOLICITANTE.

Os testes históricos de mutação não foram repetidos. A carga e o E2E foram confirmados pelo resultado do CI, sem execução local desses procedimentos nesta verificação.

As evidências registradas pertencem ao commit f4a706f9de7b5547704cdb735dbcf156b0757bb9. Se o commit final for diferente, seu CI deverá ser conferido antes da conclusão da entrega.

## Bloqueadores encontrados (em ordem de gravidade)

Nenhum bloqueador na inicialização, no login admin ou na execução local do Maven.

O fechamento documental foi preparado, mas ainda depende da incorporação e conferência dos textos no repositório, da definição da tag e da confirmação da decisão e dos responsáveis pela equipe.
