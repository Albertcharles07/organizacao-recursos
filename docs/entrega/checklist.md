# Checklist da entrega (clone limpo)

Roteiro da Aula 14, etapa 1. **Quem verifica precisa clonar o repositório do zero, numa pasta nova, e seguir só o README**, sem reaproveitar nada da máquina de quem desenvolveu. Nesta etapa só se anota; nada é corrigido.

- Data da verificação: _____
- Quem verificou: _____
- Commit verificado: _____
- Sistema operacional e versão do Docker: _____

## Artefatos

A coluna "Existe e caminho" foi preenchida a partir do repositório em 05/11/2026. As colunas "Atualizado" e "Conferido no clone" ficam para quem fizer a verificação.

| Item exigido | Existe e caminho | Atualizado? | Conferido no clone? |
| --- | --- | --- | --- |
| README com instalação reproduzível | `README.md` | | |
| Aplicação sobe só com o README (`docker compose up --build -d`) | `compose.yaml`, `Dockerfile`, `.env.example` | | |
| `mvn -B verify` passa | `pom.xml` | | |
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

Observação para a verificação: o projeto não tem Maven Wrapper (`mvnw`); o README usa `mvn`, que precisa estar instalado (Maven 3.9 ou mais novo).

## Bloqueadores encontrados (em ordem de gravidade)

1. _____
2. _____
