# Checklist da entrega (clone limpo)

Roteiro da Aula 14, etapa 1. A verificação foi feita em um clone novo do repositório. A aplicação iniciou seguindo a seção “Começar pelo Docker” do README, e os 211 testes do Maven passaram.

- Data da verificação: 12/11/2026
- Quem verificou: Carlos Daniel de Souza Gomes
- Versão candidata: [`v1.0.0-rc1`](https://github.com/Albertcharles07/organizacao-recursos/tree/v1.0.0-rc1)
- Commit verificado e marcado pela tag: [`f4a706f9de7b5547704cdb735dbcf156b0757bb9`](https://github.com/Albertcharles07/organizacao-recursos/commit/f4a706f9de7b5547704cdb735dbcf156b0757bb9)
- CI do mesmo commit: [Qualidade #23 — aprovado](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609)
- Ambiente local: Windows, Docker 29.9.2 (build `a82e528`) e Java 21.0.13

## Artefatos

A coluna “Existe e caminho” foi preenchida originalmente em 05/11/2026. Em 12/11/2026, os arquivos foram conferidos no clone e as evidências foram consultadas no GitHub, com apoio do ChatGPT. A coluna “Atualizado?” descreve o conteúdo encontrado no commit avaliado. A última coluna indica se houve leitura do documento, execução local ou consulta ao CI.

| Item exigido | Existe e caminho | Atualizado? | Conferido no clone? |
| --- | --- | --- | --- |
| Repositório com revisão por pares | PRs #2 a #12 no GitHub | Sim. Cada PR foi aprovado por outro integrante antes do merge, com merge commit. Item incluído em 12/11/2026 pela auditoria, porque o roteiro da Aula 14 o exige. | Conferido no GitHub (histórico de PRs) |
| README com instalação reproduzível | `README.md` | Sim para o fluxo testado. As instruções permitiram iniciar a aplicação e entrar como admin. | Sim — instruções executadas |
| Aplicação sobe só com o README | `compose.yaml`, `Dockerfile`, `.env.example` | Sim. A aplicação iniciou com `docker compose up --build -d`. | Sim — inicialização e login |
| `./mvnw -B verify` passa | `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Sim. O Maven terminou com `BUILD SUCCESS` e 211 testes aprovados. | Sim — comando executado |
| Documentação da API | `docs/API.md` | Os métodos e caminhos de recursos, usuários, reservas, manutenção e relatórios correspondem às declarações do `ApiController`. A comparação cobriu as rotas, sem testar individualmente todos os exemplos de resposta. | Sim — documento e rotas |
| RTM | `docs/rtm.md` | Parcial. Liga requisitos a testes e evidências. A referência principal ainda é o CI #21. Atualizar para o commit da RC1 e o CI #23. | Sim — documento |
| Diagramas de contexto, componentes e sequências críticas | `docs/ARQUITETURA.md` | Há diagramas de contexto, componentes, concorrência e material restrito. No fluxo de concorrência, foram confirmados o `FOR UPDATE` no Java e a restrição `EXCLUDE` no PostgreSQL. | Sim — documento e implementação da concorrência |
| Plano de testes | `docs/PLANO-TESTES.md` | Parcial. A tabela resume a carga em 50 usuários. O workflow executa rodadas com 10, 50 e 100. O cenário BDD está em `docs/bdd/aprovacao.feature`. | Sim — plano e workflow |
| Relatório de testes e de qualidade | `docs/relatorio-qualidade.md` | Parcial. O total de 211 testes coincide com a execução local. Falta registrar o clone limpo concluído e a execução #23. | Sim — documento e resultados locais |
| Pipeline de CI | `.github/workflows/ci.yml` | Conferido. Os jobs `tests`, `e2e` e `load` passaram na execução #23. As metas de cobertura e o quality gate também passaram. | Sim — workflow local e execução no GitHub |
| Relatórios do JaCoCo | `target/site/jacoco/index.html` e artifact `testes-e-cobertura` | Sim. O relatório local foi aberto e o relatório do artifact do mesmo commit foi conferido. | Sim — relatório local e artifact do CI |
| Relatório do SonarCloud | Etapa “SonarCloud e quality gate” do CI | O quality gate passou na execução #23. A nota e as métricas detalhadas dessa análise não foram conferidas no painel. | Verificação no CI |
| Plano e resultado da carga | `docs/PLANO-CARGA.md`, `docs/performance/resultado-01.md` | Parcial. A carga passou no CI #23. O plano ainda diz “Sem resultado medido nesta entrega”, embora apresente resultados. Os percentis do relatório pertencem às execuções históricas identificadas no arquivo. | Sim — documentos locais e resultado do CI |
| Registro de defeitos | `docs/defeitos/README.md` | O registro identifica as correções realizadas. D06 permanece aberto e P05 consta como limitação conhecida. | Sim — documento |
| Matriz de autorização | `docs/seguranca/matriz-autorizacao.md` | Contém 30 operações. A equipe ainda precisa decidir quem pode criar reservas (DV01) e confirmar quem pode consultar os motivos de manutenção (OB02). | Sim — documento |
| Mutação manual | `docs/testes/mutacao-manual.md` | O relatório registra 10 de 10 mutações detectadas na base `0170b7b`. A execução é histórica. | Sim — relatório lido, sem reexecução |
| Status dos requisitos | `docs/status-requisitos.md` | Os 14 requisitos funcionais estão mapeados. Falta incluir o resultado do clone limpo e a referência da RC1. | Sim — documento |
| Índice de evidências | `docs/evidencias/README.md` | Parcial. No commit da RC1, o índice ainda cita o CI #21 e percentuais históricos. Foi preparado um texto com os links da execução #23. | Sim — documento |
| Decisão de qualidade | `docs/decisao-qualidade.md` | No commit da RC1, o documento está em rascunho. A tag, o SHA e o CI já estão identificados. Falta registrar a decisão da equipe e os responsáveis pelos riscos. | Sim — documento |
| `CHANGELOG.md` | `CHANGELOG.md` | Registra funcionalidades, 211 testes e mudanças no CI. No commit avaliado, a RC1 ainda aparece como “a criar”. Atualizar o registro para refletir a publicação. | Sim — documento |

## Resultado da execução local

### Clone e inicialização

O repositório foi clonado na pasta nova `teste-limpo`. Com o Docker Desktop aberto, foram seguidas as instruções do README para configurar o `.env` e executar `docker compose up --build -d`.

A aplicação abriu em http://localhost:8080 e o login com o usuário `admin` funcionou. Nenhum desses passos apresentou erro. As senhas usadas no `.env` foram mantidas em sigilo.

### Testes automatizados

O Maven Wrapper executou `verify` com Java 21.0.13. A execução terminou com **BUILD SUCCESS** em **4min31s**, no dia 12/11/2026 às 16h29min29s (UTC−03:00).

| Tipo de teste | Executados | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Unitários | 32 | 0 | 0 | 0 |
| Integração | 179 | 0 | 0 | 0 |
| **Total** | **211** | **0** | **0** | **0** |

### Cobertura JaCoCo

O comando `Test-Path .\target\site\jacoco\index.html` retornou `True`. O arquivo foi aberto no navegador e apresentou os seguintes resultados:

| Métrica | Resultado local |
| --- | --- |
| Instruções | 98% de cobertura, conforme o HTML |
| Ramificações | 163 de 176 cobertas (92,61%). O HTML exibe 92%. |
| Linhas | 371 de 380 cobertas (97,63%) |
| Classes analisadas | 17 |

## Evidências do GitHub Actions

A execução [Qualidade #23](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609) passou no commit `f4a706f`. A tag `v1.0.0-rc1` foi publicada posteriormente apontando para esse mesmo commit.

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Testes e cobertura | O job `tests` e a etapa de metas de cobertura passaram. O relatório JaCoCo do artifact foi conferido. | [Artifact testes-e-cobertura](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13811152261) |
| SonarCloud | A etapa “SonarCloud e quality gate” passou. | [Execução #23, job tests](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609) |
| Testes no navegador | O job `e2e` passou. | [Artifact e2e](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13812235058) |
| Carga | O job `load` passou, incluindo as rodadas de 10, 50 e 100 usuários e o resumo. O conteúdo detalhado do artifact não foi inspecionado nesta verificação. | [Artifact carga-jmeter](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13811003153) |

Os testes manuais abrangeram a abertura da aplicação e o login admin. Cadastro, reservas e permissões dos perfis `RESPONSAVEL` e `SOLICITANTE` não foram registrados como testes manuais. Os resultados de E2E e carga vêm do CI. As mutações manuais permanecem identificadas pelo commit histórico em que foram executadas.

## Publicação da versão candidata

Em 12/11/2026, Carlos Daniel criou a tag anotada `v1.0.0-rc1` no commit testado e a enviou ao GitHub. O comando `git push origin refs/tags/v1.0.0-rc1` confirmou a criação da tag remota.

No commit marcado pela RC1, o arquivo `docs/entrega/checklist.md` ainda era o modelo em branco. O checklist preenchido foi registrado posteriormente na branch `docs/declaracoes`. Esta revisão documenta o teste da RC1 e precisa ser integrada à `main` para constar na documentação principal. Essa integração preserva a tag existente no commit que foi testado.

## Fechamento documental

Foi preparada a proposta técnica de **liberar a RC1 com ressalvas**, com base no clone limpo, nos 211 testes aprovados, na cobertura medida e no CI verde. A decisão final e a aceitação dos riscos cabem à equipe.

Também foi preparado um índice de evidências com os links do CI #23. A tentativa de gravar os textos da decisão e do índice pela integração do GitHub retornou `403`. A publicação da tag foi concluída pelo terminal. A incorporação desses dois textos deve ser conferida separadamente.

### Pendências

1. Integrar à `main` o checklist preenchido e as declarações dos integrantes que estão em `docs/declaracoes`.
2. Registrar em `docs/decisao-qualidade.md` a tag `v1.0.0-rc1`, o SHA `f4a706f9de7b5547704cdb735dbcf156b0757bb9`, o link da execução #23, a data da decisão, a justificativa e os responsáveis pelos riscos.
3. Atualizar a RTM, o relatório de qualidade, o status dos requisitos e o índice de evidências com os resultados do clone limpo e as referências da RC1. Registrar a publicação da tag no `CHANGELOG.md`.
4. Alinhar a descrição da carga no plano de testes às três rodadas do CI. No plano de carga, corrigir a frase sobre a ausência de resultados e manter cada medição histórica associada ao seu commit.
5. Confirmar as decisões sobre DV01 e OB02 e registrar o tratamento de D06, de P05 e da limitação de medir a carga na mesma máquina da aplicação.
6. Se a equipe escolher entregar um commit posterior à RC1, conferir o CI desse novo commit e registrar a versão correspondente.

## Bloqueadores encontrados

**Nenhum bloqueador técnico no clone limpo.** A aplicação abriu, o login admin funcionou e o Maven concluiu os testes com sucesso.

A tag da versão testada está publicada e vinculada ao CI aprovado. Para o fechamento formal, restam a integração dos documentos e a confirmação da decisão de qualidade pela equipe.
