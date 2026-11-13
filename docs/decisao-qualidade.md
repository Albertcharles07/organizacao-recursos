# Decisão de qualidade: v1.0.0-rc1

- Versão avaliada: [`v1.0.0-rc1`](https://github.com/Albertcharles07/organizacao-recursos/tree/v1.0.0-rc1) (tag anotada, criada em 12/11/2026)
- Commit da tag: [`f4a706f`](https://github.com/Albertcharles07/organizacao-recursos/commit/f4a706f9de7b5547704cdb735dbcf156b0757bb9) (`f4a706f9de7b5547704cdb735dbcf156b0757bb9`)
- Execução do CI no commit da tag: [Qualidade #23](https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609), com os jobs `tests`, `e2e` e `load` aprovados
- Data da decisão: 12/11/2026, pela equipe

## Decisão

☐ Liberar  ☒ **Liberar com ressalvas**  ☐ Bloquear

Justificativa: todos os critérios da tabela abaixo foram atendidos no commit da tag. O CI #23 passou com a suíte completa, E2E, carga e quality gate do SonarCloud. Num clone limpo, a aplicação subiu seguindo só o README e o `./mvnw -B verify` passou com 211 testes. As ressalvas são as limitações conhecidas da seção "Riscos aceitos", que não afetam as regras críticas (dupla reserva, sobreposição, aprovação, autorização e auditoria), mas impedem declarar o sistema pronto para produção. A evidência vem de um ambiente de CI e de um clone local: não houve teste em ambiente de produção, com volume real de dados ou com integração real de notificação.

## Critérios e evidência

| Critério | Meta | No commit da tag (`f4a706f`) | Evidência | Atendido? |
| --- | --- | --- | --- | --- |
| Testes automatizados | 100% passando | 211 de 211, 0 falhas | CI #23 e `./mvnw -B verify` no clone limpo (`docs/entrega/checklist.md`) | Sim |
| Cobertura de linhas | ≥ 80% | 97,63% (371 de 380) | JaCoCo no clone limpo; meta de cobertura aprovada no CI #23 | Sim |
| Cobertura de branches | ≥ 70% | 92,61% (163 de 176) | JaCoCo no clone limpo; meta de cobertura aprovada no CI #23 | Sim |
| Bugs e vulnerabilidades críticas (SonarCloud) | 0 | 0 issues; nota de segurança A | Análise da `main` de 10/11/2026; o código do commit da tag é o mesmo (o PR #10 só mudou documentação) | Sim |
| Quality gate do SonarCloud | Verde | Aprovado | Passo "SonarCloud e quality gate" do CI #23 | Sim |
| E2E no navegador | Passando | Passando | Job `e2e` do CI #23 | Sim |
| Dupla reserva sob carga | 1 reserva por rodada | 1 reserva com 10, 50 e 100 usuários | Job `load` do CI #23 (o job falha se aceitar mais de 1 reserva por rodada) | Sim |
| Mutações das regras críticas | Todas detectadas | 10 de 10 | `docs/testes/mutacao-manual.md` (execução de 05/11/2026; as regras críticas não mudaram depois) | Sim |
| Requisitos críticos na RTM com evidência | 100% | 100% | `docs/rtm.md` | Sim |
| Aplicação sobe num clone limpo seguindo o README | Sim | Sim, sem bloqueadores | `docs/entrega/checklist.md` (Carlos, 12/11/2026) | Sim |
| Revisão por pares | PRs aprovados antes do merge | PRs #2 a #11 aprovados e integrados com merge commit | Histórico de PRs do repositório | Sim |
| Repositório público | Sim | Sim | Desde 06/11/2026 | Sim |

## Riscos aceitos

| Risco | Impacto | Justificativa | Responsável |
| --- | --- | --- | --- |
| P05: webhook de notificação sem nova tentativa após falha | A notificação externa pode ficar como FALHOU; a reserva não é afetada | Notificação simulada atende ao escopo (RF11); a falha fica registrada e testada (`NotificacaoEntregaIT`) | Charles Elias de Araújo |
| Gerador de carga e aplicação na mesma máquina do CI | Tempos medidos não isolam a aplicação | O objetivo do teste é a consistência sob disputa, que foi comprovada nas três rodadas | Carlos Daniel de Souza Gomes |
| DV01: Responsável e Administrador também criam reservas (a especificação cita só o Solicitante) | Perfis de gestão podem reservar recursos | Decisão da equipe: manter, porque gestores também precisam reservar; o comportamento está fixado na matriz de autorização (M13) | João Pedro Dias Araújo |
| OB02: qualquer usuário logado vê os bloqueios de manutenção e o motivo | O motivo da manutenção fica visível para o Solicitante | Decisão da equipe: manter, para o Solicitante saber por que um recurso está indisponível; o motivo não deve conter dado pessoal | João Pedro Dias Araújo |
| D06: `Rules.overlaps` não é usado pela aplicação | Teste unitário que não protege o sistema | A proteção real é a restrição do banco, coberta por testes de integração e mutação. Remover a função mudaria o código depois da tag | Lucas Rodrigues de Magalhaes |

## Declaração dos integrantes

Cada integrante escreveu e commitou a própria declaração, com as próprias palavras, informando o que fez no projeto e o uso de ferramentas de IA.

| Integrante | O que fez | Uso de IA |
| --- | --- | --- |
| Lucas Rodrigues de Magalhaes | Responsável pela auditoria e pelos testes (QA): defini o que testar, conferi os resultados no CI e no SonarCloud, registrei defeitos e evidências e integrei os PRs na main. | Usei o Claude para gerar testes, correções e documentação. Revisei e validei cada mudança no CI antes do merge. |
| Carlos Daniel de Souza Gomes | Participei da organização e publicação do projeto no GitHub, da configuração e execução da aplicação com Docker e banco de dados, da verificação do funcionamento do sistema e da análise dos testes e Pull Requests para identificar problemas e melhorias. | Utilizei o ChatGPT como apoio para analisar códigos, orientar configurações, solucionar erros e revisar testes. Conferi os resultados por meio de execuções locais, logs do Docker e análise das evidências disponíveis no GitHub. |
| João Pedro Dias Araújo | Participei do desenvolvimento do frontend, incluindo a organização das telas e dos formulários e sua comunicação com o backend. Também colaborei na configuração do ambiente Docker e em ajustes de configuração do backend, como variáveis de ambiente, conexão com o PostgreSQL e porta da aplicação. | Utilizei o ChatGPT como apoio para gerar e ajustar o código da interface, compreender as configurações do Docker e orientar os ajustes de integração entre frontend, backend e banco de dados. |
| Charles Elias de Araújo | Fui responsável pela criação do repositório do projeto no GitHub, publicação e envio dos arquivos do projeto para a plataforma, organização inicial da estrutura do repositório e inclusão dos integrantes da equipe como colaboradores. Também participei do acompanhamento dos Pull Requests, revisão das alterações e aprovação dos PRs sob minha responsabilidade. | Utilizei o ChatGPT como ferramenta de apoio para auxiliar na criação e organização do repositório, configuração do Git e GitHub, resolução de problemas durante o versionamento e orientação sobre o gerenciamento de Pull Requests e organização do projeto. |

## Texto para o formulário de entrega

> Grupo: Carlos Daniel de Souza Gomes, Charles Elias de Araújo, João Pedro Dias Araújo e Lucas Rodrigues de Magalhaes. Repositório público: https://github.com/Albertcharles07/organizacao-recursos. Versão avaliada: tag `v1.0.0-rc1` (commit `f4a706f`), com CI aprovado em https://github.com/Albertcharles07/organizacao-recursos/actions/runs/42920021609. Decisão de qualidade: liberar com ressalvas (`docs/decisao-qualidade.md`).
