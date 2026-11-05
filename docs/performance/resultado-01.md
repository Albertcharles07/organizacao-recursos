# Resultado do teste de carga 01

Executado em **05/11/2026** pelo job `load` do GitHub Actions, commit `0170b7b` (branch `auditoria/fechamento`, PR de validação #2 no fork `lucasrmagalhaess/organizacao-recursos`). Plano em `docs/PLANO-CARGA.md`; script em `load/reservas.jmx`.

## Resultado

**Aprovado nas três rodadas.** Em todas, exatamente uma reserva foi aceita, todas as outras foram recusadas com 409, nenhuma resposta teve outro código e o banco gravou uma única reserva para o recurso disputado.

| Usuários simultâneos | Pedidos | 200 (aceita) | 409 (conflito) | Outros códigos | p95 da reserva | Reservas gravadas no banco | Resultado |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 10 | 10 | 1 | 9 | 0 | 191 ms | 1 | OK |
| 50 | 50 | 1 | 49 | 0 | 366 ms | 1 | OK |
| 100 | 100 | 1 | 99 | 0 | 487 ms | 1 | OK |

A meta proposta no plano era p95 abaixo de 2000 ms e nenhum erro 5xx: atendida em todas as rodadas. O p95 sobe com o número de usuários (191 → 366 → 487 ms) porque os pedidos esperam o bloqueio do recurso (`select ... for update`) e a verificação da restrição de exclusão, que serializam as reservas do mesmo recurso.

## Repetição

O job `load` rodou de novo no commit `2801221`, numa máquina diferente do GitHub Actions (2 vCPU Intel Xeon 6973P-C), com o mesmo resultado de consistência:

| Usuários simultâneos | 200 (aceita) | 409 (conflito) | Outros códigos | p95 da reserva | Reservas gravadas no banco | Resultado |
| --- | --- | --- | --- | --- | --- | --- |
| 10 | 1 | 9 | 0 | 203 ms | 1 | OK |
| 50 | 1 | 49 | 0 | 353 ms | 1 | OK |
| 100 | 1 | 99 | 0 | 471 ms | 1 | OK |

Entre as duas execuções, o p95 variou no máximo 16 ms por rodada.

## Como foi executado

- Cada rodada cria um recurso novo com `load/prepare.py`, autentica cada usuário virtual (token CSRF e login) e dispara todas as reservas juntas, com uma barreira de sincronização (`SyncTimer`) que espera até 30 s.
- A contagem no banco foi feita com `select count(*) from allocation where resource_id=<recurso> and active and booking_id is not null` direto no PostgreSQL da rodada.
- Verificação automática: `.github/scripts/resumo_carga.py` falha o job se alguma rodada não tiver exatamente 1 aceita, só 409 nas demais e 1 reserva no banco.

## Ambiente

| Item | Valor |
| --- | --- |
| Máquina | Runner `ubuntu-24.04` do GitHub Actions: 2 vCPU AMD EPYC 9V45, 7,7 GiB de RAM |
| Aplicação | `docker compose up --build` (Java 21, Spring Boot 3.5.16, PostgreSQL 16) |
| Gerador de carga | Apache JMeter 5.6.3 na mesma máquina da aplicação |
| Rampa | 5 s, um laço por usuário |

## Limitações

- Gerador de carga e aplicação dividem a mesma máquina de 2 vCPU: os tempos medem o sistema inteiro sob disputa, não a aplicação isolada.
- Uma execução por quantidade de usuários. Para comparar versões, repetir o job e registrar um `resultado-02.md`.
- O cenário mede o caso crítico (muitos pedidos para o mesmo recurso e horário), não carga distribuída por vários recursos.

Evidência: anotações e resumo do job `load` na execução citada, e o artifact `carga-jmeter` com os arquivos `.jtl`, os relatórios HTML de cada rodada e a contagem do banco.
