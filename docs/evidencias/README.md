# Índice de evidências

Todas as evidências da entrega, com o lugar onde cada uma está. Na entrega final, cada link deve apontar para a execução do CI **no commit da tag `v1.0.0-rc1`** no repositório do grupo; as execuções abaixo são as de 05/11/2026, feitas no fork de validação e no repositório do grupo antes da tag.

| Evidência | Onde está | Situação em 05/11/2026 |
| --- | --- | --- |
| Execução do CI (testes, cobertura, E2E e carga) | Aba Actions do repositório, workflow "Qualidade"; anotação "Resumo dos testes" de cada execução | 208 testes passando, commit `be6fda0` (fork) |
| Relatório do JaCoCo | Artifact `testes-e-cobertura` de cada execução; cópias em `docs/evidencias/ci-qualidade-1/` e `ci-qualidade-2/` | 97,65% linhas, 92,53% branches |
| Quality gate do SonarCloud | Passo "SonarCloud e quality gate" do CI | **Pendente**: `SONAR_TOKEN` |
| Resultado da carga | `docs/performance/resultado-01.md`; artifact `carga-jmeter` | Aprovado |
| Mutação manual | `docs/testes/mutacao-manual.md` | 10 de 10 detectadas |
| Matriz de autorização | `docs/seguranca/matriz-autorizacao.md` | 30 operações × 4 perfis passando |
| Status dos requisitos | `docs/status-requisitos.md` | 14 de 14 feitos; SonarCloud pendente |
| RTM | `docs/rtm.md` | 100% dos críticos com evidência |
| Defeitos | `docs/defeitos/README.md` | D01 a D05, D07, P01 e P02 fechados |
| Relatório de qualidade | `docs/relatorio-qualidade.md` | Ainda não liberado |
| Resumo legível por máquina | `docs/evidencias/resumo.json` | Todas as execuções |
| Execução local antiga (sem Docker) | `docs/evidencias/local-2026-10-23/` | Histórico; substituída pelo CI |

## Como atualizar na Aula 14

1. Criar a tag anotada `v1.0.0-rc1` no commit avaliado da `main` e enviá-la.
2. Abrir a execução do CI desse commit e copiar o link para a primeira linha da tabela.
3. Conferir que o quality gate do SonarCloud aparece verde e colocar o link na terceira linha.
4. Atualizar a coluna "Situação" com os números dessa execução, sem copiar números de execuções antigas.
