# Checklist da entrega (clone limpo)

Roteiro da Aula 14, etapa 1. **Quem verifica precisa clonar o repositório do zero, numa pasta nova, e seguir só o README**, sem reaproveitar nada da máquina de quem desenvolveu. Nesta etapa só se anota; nada é corrigido.

- Data da verificação: 12/11/2026
- Quem verificou: Carlos Daniel de Souza Gomes
- Commit verificado: f4a706f9de7b5547704cdb735dbcf156b0757bb9
- Sistema operacional e versão do Docker: Windows; Docker 29.9.2, build a82e528

## Artefatos

A coluna "Existe e caminho" foi preenchida a partir do repositório em 05/11/2026. As colunas "Atualizado" e "Conferido no clone" ficam para quem fizer a verificação.

| Item exigido | Existe e caminho | Atualizado? | Conferido no clone? |
| --- | --- | --- | --- |
| README com instalação reproduzível | `README.md` | | Sim |
| Aplicação sobe só com o README (`docker compose up --build -d`) | `compose.yaml`, `Dockerfile`, `.env.example` | | Sim |
| `./mvnw -B verify` passa | `pom.xml`, `mvnw`, `.mvn/wrapper/` | | Não conferido |
| Documentação da API | `docs/API.md` | | |
| RTM | `docs/rtm.md` | | |
| Diagramas de contexto, componentes e sequências críticas | `docs/ARQUITETURA.md` | | |
| Plano de testes | `docs/PLANO-TESTES.md` | | |
| Relatório de testes e de qualidade | `docs/relatorio-qualidade.md` | | |
| Pipeline de CI | `.github/workflows/ci.yml` | | |
| Relatórios do JaCoCo | Artifact `testes-e-cobertura` do CI | | |
| Relatório do SonarCloud | Passo do SonarCloud no CI | | |
| Plano e resultado da carga | `docs/PLANO-CARGA.md`, `docs/performance/resultado-01.md` | | |
| Registro de defeitos | `docs/defeitos/README.md` | | |
| Matriz de autorização | `docs/seguranca/matriz-autorizacao.md` | | |
| Mutação manual | `docs/testes/mutacao-manual.md` | | |
| Status dos requisitos | `docs/status-requisitos.md` | | |
| Índice de evidências | `docs/evidencias/README.md` | | |
| Decisão de qualidade | `docs/decisao-qualidade.md` | | |
| `CHANGELOG.md` | `CHANGELOG.md` | | |

Observação para a verificação: o projeto usa o Maven Wrapper; não é preciso ter o Maven instalado, só o JDK 21.

### Resultado da execução

A aplicação iniciou normalmente seguindo a seção “Começar pelo Docker” do README. Foi possível acessar http://localhost:8080 e entrar com o usuário admin. Não ocorreram erros durante esses passos.

O comando de verificação do Maven não foi executado. Os demais artefatos e sua atualização não foram avaliados nesta verificação.

## Bloqueadores encontrados (em ordem de gravidade)

Nenhum nos passos executados.
