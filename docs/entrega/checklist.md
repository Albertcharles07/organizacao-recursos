# Checklist da entrega (clone limpo)

Roteiro da Aula 14, etapa 1. **Quem verifica precisa clonar o repositório do zero, numa pasta nova, e seguir só o README**, sem reaproveitar nada da máquina de quem desenvolveu. Nesta etapa só se anota; nada é corrigido.

- Data da verificação: 12/11/2026
- Quem verificou: Carlos Daniel de Souza Gomes
- Commit verificado: f4a706f9de7b5547704cdb735dbcf156b0757bb9
- Sistema operacional e versão do Docker: Windows; Docker 29.9.2, build a82e528
- Java utilizado na verificação Maven: 21.0.13

## Artefatos

A coluna "Existe e caminho" foi preenchida a partir do repositório em 05/11/2026. Os resultados abaixo registram apenas as verificações realizadas. “Não avaliado” e “Não conferido” não significam reprovação.

| Item exigido | Existe e caminho | Atualizado? | Conferido no clone? |
| --- | --- | --- | --- |
| README com instalação reproduzível | `README.md` | Sim — instruções de inicialização validadas | Sim |
| Aplicação sobe só com o README (`docker compose up --build -d`) | `compose.yaml`, `Dockerfile`, `.env.example` | Sim — inicialização validada | Sim |
| `./mvnw -B verify` passa | `pom.xml`, `mvnw`, `.mvn/wrapper/` | Sim — execução concluída com sucesso | Sim |
| Documentação da API | `docs/API.md` | Não avaliado | Não conferido |
| RTM | `docs/rtm.md` | Não avaliado | Não conferido |
| Diagramas de contexto, componentes e sequências críticas | `docs/ARQUITETURA.md` | Não avaliado | Não conferido |
| Plano de testes | `docs/PLANO-TESTES.md` | Não avaliado | Não conferido |
| Relatório de testes e de qualidade | `docs/relatorio-qualidade.md` | Não avaliado | Não conferido |
| Pipeline de CI | `.github/workflows/ci.yml` | Execução Qualidade #23 aprovada no commit verificado; arquivo não revisado | Não conferido |
| Relatórios do JaCoCo | Local: `target/site/jacoco/index.html`; CI: artifact `testes-e-cobertura` | Sim — resultados vinculados ao commit verificado | Sim — relatório local e artifact do CI conferidos |
| Relatório do SonarCloud | Passo do SonarCloud no CI | Não avaliado | Não conferido |
| Plano e resultado da carga | `docs/PLANO-CARGA.md`, `docs/performance/resultado-01.md` | Não avaliado | Não conferido |
| Registro de defeitos | `docs/defeitos/README.md` | Não avaliado | Não conferido |
| Matriz de autorização | `docs/seguranca/matriz-autorizacao.md` | Não avaliado | Não conferido |
| Mutação manual | `docs/testes/mutacao-manual.md` | Não avaliado | Não conferido |
| Status dos requisitos | `docs/status-requisitos.md` | Não avaliado | Não conferido |
| Índice de evidências | `docs/evidencias/README.md` | Não avaliado | Não conferido |
| Decisão de qualidade | `docs/decisao-qualidade.md` | Não avaliado | Não conferido |
| `CHANGELOG.md` | `CHANGELOG.md` | Não avaliado | Não conferido |

Observação para a verificação: o projeto usa o Maven Wrapper; não é preciso ter o Maven instalado, só o JDK 21.

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
- Cobertura de ramificações: 92%.
- Linhas cobertas: 371 de 380, aproximadamente 97,6%.
- Classes analisadas: 17.

### Evidência do GitHub Actions

Foi identificada a execução Qualidade #23, concluída com sucesso e referente ao mesmo commit verificado:

https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609

O relatório no artifact `testes-e-cobertura` dessa execução também foi conferido.

### Limites da verificação

Os demais artefatos e sua atualização não foram avaliados nesta verificação. Não foram registrados testes manuais de cadastro, reservas ou permissões dos perfis RESPONSAVEL e SOLICITANTE.

## Bloqueadores encontrados (em ordem de gravidade)

Nenhum nos passos executados.
