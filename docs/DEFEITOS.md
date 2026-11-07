# Registro de defeitos e pendências

Os itens corrigidos abaixo resultam de revisão do código durante a criação. Não são alegações de testes de integração executados. A confirmação por ambiente real permanece registrada separadamente.

| ID | Tipo | Prioridade | Reprodução / risco | Ação | Estado / evidência |
|---|---|---|---|---|---|
| D01 | Regra | Alta | Concluir reserva com material retirado sem devolução | Verificação de movimentos antes de CONCLUIDA | **Fechado**: `startedBookingAndMaterialLifecycle` passou no CI "Qualidade #1" (fff753c) e "#2" (00c121f) |
| D02 | Segurança | Alta | Conta desativada conservar sessão anterior | ActiveAccountFilter verifica conta em cada request | **Fechado**: `adminResourceAndUserFlows` passou no CI "Qualidade #1" e "#2" |
| D03 | Regra | Alta | Cancelar reserva cujo início passou antes de marcar EM_USO | Comparar início com relógio antes de cancelar/alterar | **Fechado**: `startedBookingAndMaterialLifecycle` (cancelamento após o início recusado) passou no CI "Qualidade #1" e "#2" |
| D04 | Integridade | Alta | Editar restrição/tipo de recurso com reserva futura já aprovada | Impedir edição com alocação futura ativa | Corrigido por inspeção; **não havia teste de regressão**. Adicionado `ResourceApiIT.regressaoD04RecursoComReservaFuturaNaoPodeSerEditado`; fechar quando passar no CI |
| P01 | Ambiente | Bloqueante para homologação | Ambiente local sem Docker e Java 21 | Executar CI/Compose no ambiente alvo | **Fechado**: CI no GitHub Actions executa a suíte completa com Java 21 e Docker ("Qualidade #1" e "#2") |
| P02 | Qualidade | Alta | Cobertura unitária parcial abaixo da meta global | Rodar IT e medir cobertura completa; ampliar se necessário | **Fechado**: 95,40% de linhas e 84,81% de branches no CI "Qualidade #2" |
| P03 | Integração | Alta | Projeto SonarCloud ainda sem conta/secret da equipe | Conectar repositório e quality gate | Aberto: só o dono do repositório pode criar o secret `SONAR_TOKEN`; o passo falha em todas as execuções do CI |
| P04 | Processo | Alta | Sem revisões/autoria dos integrantes | PRs reais e revisão por colega | Em andamento: PRs #2 e #3 abertos aguardando revisão |
| P05 | Robustez | Média | Webhook sem retry após falha/queda | Simulação interna atende escopo; evoluir para outbox | Limitação documentada |
| D05 | Ambiente/CI | Alta | Job `e2e` falhou no CI do PR #2 (execução "Qualidade #2", commit 00c121f) no passo `npx playwright install --with-deps chromium`: "Playwright does not support chromium on ubuntu26.04-x64". Causa raiz: o rótulo `ubuntu-latest` do GitHub passou a apontar para Ubuntu 26.04 a partir de 19/10/2026; o Playwright 1.55.1 do projeto não suporta essa versão. Na execução "Qualidade #1" (25/10, commit fff753c) o mesmo job passou. | Fixar `runs-on: ubuntu-24.04` nos jobs `tests` e `e2e` para o ambiente de CI ser reprodutível | **Fechado**: job `e2e` verde no CI do PR #3 (commit bb48527) |

## Modelo para novos defeitos

ID, requisito RTM, versão/commit, ambiente, pré-condições, passos, esperado, observado, severidade, prioridade, responsável, evidência, causa raiz, correção, teste de regressão e resultado da reexecução. Não fechar somente porque a alteração foi escrita.
