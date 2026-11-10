# Documentação

Índice dos documentos do projeto Organização de Recursos. Para instalar e rodar, comece pelo `README.md` da raiz.

## Requisitos e regras

| Documento | Conteúdo |
| --- | --- |
| [`REGRAS.md`](REGRAS.md) | Regras de negócio, estados da reserva e permissões |
| [`status-requisitos.md`](status-requisitos.md) | Situação de cada um dos 14 requisitos, com teste e risco |
| [`rtm.md`](rtm.md) | Rastreabilidade: requisito, risco, implementação, teste, mutação e evidência |
| [`bdd/aprovacao.feature`](bdd/aprovacao.feature) | Cenário BDD da aprovação de recurso restrito |

## Arquitetura e API

| Documento | Conteúdo |
| --- | --- |
| [`ARQUITETURA.md`](ARQUITETURA.md) | Diagramas de contexto, componentes e sequências críticas, utility tree (ATAM) e ADRs |
| [`API.md`](API.md) | Endpoints, autenticação, CSRF, relatório de utilização e histórico de estados |

## Testes e qualidade

| Documento | Conteúdo |
| --- | --- |
| [`PLANO-TESTES.md`](PLANO-TESTES.md) | Estratégia, níveis de teste, partições e critérios |
| [`seguranca/matriz-autorizacao.md`](seguranca/matriz-autorizacao.md) | Matriz perfil × operação, executada pelo `AutorizacaoMatrizIT` |
| [`testes/mutacao-manual.md`](testes/mutacao-manual.md) | Regras críticas quebradas de propósito e testes que detectaram |
| [`PLANO-CARGA.md`](PLANO-CARGA.md) | Plano do teste de carga com JMeter |
| [`performance/resultado-01.md`](performance/resultado-01.md) | Resultado do teste de carga |
| [`defeitos/README.md`](defeitos/README.md) | Registro de defeitos, com evidência de correção |
| [`relatorio-qualidade.md`](relatorio-qualidade.md) | Relatório de qualidade e pendências |

## Entrega

| Documento | Conteúdo |
| --- | --- |
| [`evidencias/README.md`](evidencias/README.md) | Índice das evidências de execução |
| [`entrega/checklist.md`](entrega/checklist.md) | Checklist de clone limpo (Aula 14) |
| [`decisao-qualidade.md`](decisao-qualidade.md) | Decisão de liberação, riscos aceitos e declaração dos integrantes |
| [`ENTREGA.md`](ENTREGA.md) | Checklist da equipe |
| [`APRESENTACAO.md`](APRESENTACAO.md) e `Apresentacao-Recursos.pptx` | Roteiro e slides da apresentação |
| [`../CHANGELOG.md`](../CHANGELOG.md) | Mudanças da versão candidata |
