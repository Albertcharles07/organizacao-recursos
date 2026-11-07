## O que muda

<!-- Uma ou duas frases: qual regra, defeito ou requisito este PR atende. -->

## Requisito e risco

- Requisito na RTM (`docs/rtm.md`): <!-- ex.: RF12 -->
- Defeito relacionado (`docs/defeitos/README.md`): <!-- ex.: D05, ou "nenhum" -->

## Como foi testado

- [ ] Testes novos ou alterados: <!-- nome da classe e do teste -->
- [ ] `./mvnw -B verify` passou (local ou no CI deste PR)
- [ ] Se for TDD: o commit de teste ("red") falhou no CI antes do commit de implementação ("green")
- [ ] Se mudou rota da API: linha correspondente em `docs/seguranca/matriz-autorizacao.md`

## Evidência

<!-- Link da execução do CI deste PR, ou print do resumo dos testes. -->

## Checklist do revisor

- [ ] Li o diff inteiro
- [ ] O teste falharia se a regra fosse quebrada
- [ ] Documentação (`docs/API.md`, RTM, defeitos) atualizada quando necessário
- [ ] Merge com "Create a merge commit" (não usar squash nem rebase)
