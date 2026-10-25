# Roteiro de apresentação e demonstração

Tempo sugerido: 10–12 minutos. Preencher nomes reais e atualizar os resultados antes da defesa. Este roteiro acompanha o código e não afirma homologação concluída.

## 1. Problema e objetivo (45 s)

“Precisamos organizar salas, professores e equipamentos sem alocar o mesmo recurso em horários sobrepostos. Nosso trabalho também precisa demonstrar qualidade por evidências.”

## 2. Perfis e regras (1 min)

Mostrar solicitante, responsável e administrador. Explicar que administrador não aprova automaticamente recurso restrito. Citar intervalos [início, fim) e unidade individual de material.

## 3. Arquitetura (1 min)

Abrir diagrama de componentes em ARQUITETURA.md. Explicar Spring Boot, PostgreSQL, frontend na mesma origem e sessões com CSRF. Citar escolha de monólito para reduzir complexidade operacional.

## 4. Concorrência (1 min)

Mostrar restrição de exclusão na migração e teste simultaneousRequestsAcceptOnlyOne. Explicar por que consultar disponibilidade antes de salvar não basta. Mostrar resultado real quando disponível.

## 5. Demonstração principal (3 min)

1. Administrador cria Sala 101, docente e Projetor 01 restrito.
2. Solicitante reserva os três recursos juntos em horário futuro.
3. Outra reserva para o mesmo docente/horário deve falhar, mesmo em sala diferente.
4. Solicitante não consegue aprovar. Responsável aprova.
5. Mostrar bloqueio de manutenção em outro intervalo e busca por disponibilidade.
6. Mostrar auditoria, notificação interna e relatório.

Para demonstrar retirada/devolução, prepare reserva com início poucos minutos à frente, aguarde o horário e use o perfil responsável. Não altere banco manualmente durante a demonstração para fingir o comportamento normal.

## 6. Qualidade e rastreabilidade (2 min)

Mostrar RTM, cenário BDD, JUnit, teste de API caixa-preta, E2E e artifacts do pipeline. Informar cobertura real da execução completa, status SonarCloud e relatório JMeter. Se houver pendência, declarar explicitamente.

## 7. Defeitos e decisão (1 min)

Explicar um defeito real com causa, correção e regressão. Mostrar riscos remanescentes: notificação sem retry e recursos individuais. Só declarar release aprovada depois de todos os critérios críticos passarem.

## Perguntas para ensaiar

- Por que o professor também é um recurso?
- Por que H2 ou mocks não comprovam a restrição PostgreSQL?
- O que acontece se o segundo recurso de uma reserva conflitar?
- Por que 409 é esperado no teste de carga concorrente?
- Qual a diferença entre cobertura de linha e branch?
- Quem pode aprovar e onde isso é validado?
- Como sabemos quem alterou o estado?
- Qual limitação permanece mesmo se a suíte estiver verde?
