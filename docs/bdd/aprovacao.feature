# language: pt
Funcionalidade: Aprovação de recurso restrito
  Como responsável
  Quero validar pedidos de recursos restritos
  Para que solicitantes não iniciem uso sem autorização

  Cenário: Solicitação exige aprovação e alteração exige nova avaliação
    Dado um material ativo que exige aprovação
    Quando Ana cria uma reserva futura desse material
    Então a reserva fica SOLICITADA
    E o administrador não pode aprová-la
    Quando um responsável aprova a solicitação
    Então a reserva fica APROVADA
    Quando Ana altera a reserva
    Então a reserva volta para SOLICITADA

# Cenário automatizado em ResourceApiIT.bddRestrictedResource (JUnit 5).
# Este arquivo é especificação; não existe runner Cucumber nesta versão.
