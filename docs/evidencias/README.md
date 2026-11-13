# Índice de evidências

Evidências da versão candidata [`v1.0.0-rc1`](https://github.com/Albertcharles07/organizacao-recursos/tree/v1.0.0-rc1), commit [`f4a706f`](https://github.com/Albertcharles07/organizacao-recursos/commit/f4a706f9de7b5547704cdb735dbcf156b0757bb9). A execução do CI desse commit é a [Qualidade #23](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609), com os jobs `tests`, `e2e` e `load` aprovados. Os documentos citados abaixo foram atualizados depois da tag, sem mudança de código, para registrar o resultado dessa execução e do clone limpo.

## Evidências da versão

| Evidência | Onde está | Resultado no commit da tag |
| --- | --- | --- |
| Execução do CI no commit da tag | [Qualidade #23](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609) | 211 testes, 0 falhas; `tests`, `e2e` e `load` aprovados |
| Relatório do JaCoCo | [Artifact `testes-e-cobertura`](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13811152261) do CI #23 | Meta de cobertura aprovada no CI; 97,63% das linhas e 92,61% dos branches medidos no clone limpo, no mesmo commit |
| Quality gate do SonarCloud | Passo "SonarCloud e quality gate" do [CI #23](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609); painel do projeto no SonarCloud | Aprovado. Última análise do painel (10/11/2026, mesmo código): 0 issues e nota de segurança A (print: `sonarcloud-main-2026-11-10.png`) |
| Testes no navegador (E2E) | [Artifact `e2e`](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13812235058) do CI #23 | Aprovado |
| Resultado da carga | [Artifact `carga-jmeter`](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609/artifacts/13811003153) do CI #23; `docs/performance/resultado-01.md` | Aprovado: 1 reserva aceita por rodada com 10, 50 e 100 usuários (o job falha se aceitar mais de uma) |
| Clone limpo | `docs/entrega/checklist.md` | Aplicação subiu seguindo só o README; `./mvnw -B verify` com 211 testes; nenhum bloqueador |
| RTM | `docs/rtm.md` | 100% dos requisitos críticos com evidência |
| Defeitos | `docs/defeitos/` | D01 a D05, D07 a D10 e P01 a P04 fechados; D06 e P05 aceitos como riscos |
| Relatório de qualidade | `docs/relatorio-qualidade.md` | Liberado com ressalvas |
| Decisão de qualidade | `docs/decisao-qualidade.md` | Liberar com ressalvas, com os riscos aceitos e as declarações dos integrantes |
| Mutação manual | `docs/testes/mutacao-manual.md` | 10 de 10 detectadas (execução de 05/11/2026; as regras críticas não mudaram depois) |
| Matriz de autorização | `docs/seguranca/matriz-autorizacao.md` | 30 operações × 4 perfis, executada no CI #23 |
| Status dos requisitos | `docs/status-requisitos.md` | 14 de 14 feitos |

## Histórico

| Evidência | Onde está | Observação |
| --- | --- | --- |
| CI da `main` antes da tag | "Qualidade #21", commit `7a037b3` (print: `ci-main-21.png`) | Mesmo código da tag; antes do PR #10, que só mudou documentação |
| Execuções no fork de validação | `docs/evidencias/resumo.json`, `ci-qualidade-1/`, `ci-qualidade-2/` | Execuções de 25/10 a 05/11/2026 |
| Execução local antiga (sem Docker) | `docs/evidencias/local-2026-10-23/` | Substituída pelo CI |

## TDD (red e green)

| Arquivo | O que mostra |
| --- | --- |
| `tdd/relatorio-utilizacao-1-red-ci.png` | Relatório de utilização: CI vermelho no commit só com os testes (red) |
| `tdd/relatorio-utilizacao-2-commits-red-green.png` | Relatório de utilização: commits de teste (red) e de implementação (green) no PR #4 |
| `tdd/historico-estados-1-red-erro-compilacao.png` | Histórico de estados: CI vermelho no commit red (o teste ainda não compila sem a implementação) |
| `tdd/historico-estados-2-green-pr6.png` | Histórico de estados: CI verde depois da implementação, no PR #6 |
