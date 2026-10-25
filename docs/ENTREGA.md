# Checklist de entrega da equipe

## Preparação

- [ ] Preencher integrantes, turma e professor no README/apresentação.
- [ ] Executar Docker Compose e validar os três perfis.
- [ ] Revisar decisões de escopo: unidade individual de material, pendentes ocupam agenda, aprovação separada do administrador.
- [ ] Criar repositório real com branches e PRs revisados por colegas.
- [ ] Conhecer as regras de autoria e uso de IA da disciplina e declarar a assistência conforme solicitado.

## Evidências que precisam ser reais

- [ ] Java 21: `mvn clean verify -Pquality` aprovado.
- [ ] JaCoCo: >=80% linhas, >=70% branches na execução completa.
- [ ] Teste concorrente: uma reserva, uma rejeição, sem skip.
- [ ] E2E desktop e móvel aprovados.
- [ ] WireMock aprovado e cenário BDD revisado com a equipe.
- [ ] GitHub Actions rodando em PR.
- [ ] SonarCloud com token/organização/projeto, sem críticos e quality gate aprovado.
- [ ] JMeter com JTL, relatório, hardware, percentis e contagem no banco.
- [ ] RTM atualizada com commit/links de execução.
- [ ] Registro de defeitos revisado e críticos encerrados por reexecução.
- [ ] Relatório final com decisão de qualidade e riscos remanescentes.
- [ ] Demonstração ensaiada por todos os integrantes.

## Organização sugerida do grupo

Uma pessoa acompanha domínio/concorrência; outra interface e fluxos; outra testes/CI; outra documentação e evidências. Todos revisam código de outra pessoa. Adapte à quantidade real de integrantes, sem inventar contribuições.

## Roteiro de revisão por pares

No PR: problema/regra, alteração, teste executado, saída/link, riscos e item RTM. Revisor deve conseguir explicar o que impede dois pedidos simultâneos e por que o teste depende de banco real. Release candidate só após checklist concluído. A entrega é na semana 46 conforme enunciado; confirme a data exata com a instituição.
