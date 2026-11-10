# Decisão de qualidade: v1.0.0-rc1

> **Rascunho preparado em 05/11/2026.** A decisão, os responsáveis pelos riscos e as declarações dos integrantes **devem ser preenchidos pela equipe na Aula 14**, depois de criar a tag e conferir as evidências no commit dela. Não preencher a decisão antes de ver o CI da tag.

- Versão avaliada: `v1.0.0-rc1`
- Commit da tag: _____
- Execução do CI no commit da tag: _____
- Data da decisão: _____

## Decisão

☐ Liberar  ☐ Liberar com ressalvas  ☐ Bloquear

Justificativa (proporcional à evidência; testes locais não sustentam "pronto para produção"): _____

## Critérios e evidência

Preencher a coluna "No commit da tag" com o resultado da execução do CI da tag. A coluna "Em 05/11/2026" mostra o último resultado conhecido, só como referência.

| Critério | Meta | Em 05/11/2026 | No commit da tag | Atendido? |
| --- | --- | --- | --- | --- |
| Testes automatizados | 100% passando | 208 de 208 (fork, `2801221`) | | |
| Cobertura de linhas | ≥ 80% | 97,65% | | |
| Cobertura de branches | ≥ 70% | 92,53% | | |
| Bugs e vulnerabilidades críticas (SonarCloud) | 0 | 0 (2 issues médias corrigidas, D09) | | |
| Quality gate do SonarCloud | Verde | Aprovado | | |
| E2E no navegador | Passando | Passando | | |
| Dupla reserva sob carga | 1 reserva por rodada | 1 em 10, 50 e 100 usuários | | |
| Mutações das regras críticas | Todas detectadas | 10 de 10 | | |
| Requisitos críticos na RTM com evidência | 100% | 100% | | |
| Aplicação sobe num clone limpo seguindo o README | Sim | Checklist ainda não feito | | |
| Repositório público | Sim | Sim (desde 06/11/2026) | | |

## Riscos aceitos

Cada risco aceito precisa de impacto, justificativa e responsável.

| Risco | Impacto | Justificativa | Responsável |
| --- | --- | --- | --- |
| P05: webhook de notificação sem nova tentativa após falha | A notificação externa pode ficar como FALHOU; a reserva não é afetada | Notificação simulada atende ao escopo (RF11); a falha fica registrada | _____ |
| D06: `Rules.overlaps` não é usado pela aplicação | Teste unitário que não protege o sistema | A proteção real é a restrição do banco, coberta por testes de integração e mutação | _____ |
| Gerador de carga e aplicação na mesma máquina do CI | Tempos medidos não isolam a aplicação | Objetivo do teste é a consistência sob disputa, que foi comprovada | _____ |
| _____ | _____ | _____ | _____ |

## Declaração dos integrantes

Cada integrante escreve, com as próprias palavras, o que fez no projeto. Declarar também o uso de ferramentas de IA, conforme as regras da disciplina.

| Integrante | O que fez | Uso de IA |
| --- | --- | --- |
| Lucas Rodrigues de Magalhaes | Responsável pela auditoria e pelos testes (QA): defini o que testar, conferi os resultados no CI e no SonarCloud, registrei defeitos e evidências e integrei os PRs na main. | Usei o Claude para gerar testes, correções e documentação. Revisei e validei cada mudança no CI antes do merge. |
| _____ | _____ | _____ |
| _____ | _____ | _____ |
| _____ | _____ | _____ |
