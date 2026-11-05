# Plano de carga JMeter

## Hipótese e critérios

Cenário principal: 50 sessões autenticadas disputam a mesma sala e o mesmo intervalo. Recurso criado exclusivamente para a rodada. Esperado: **1 resposta 200, 49 respostas 409, uma única reserva persistida**. HTTP 409 é conflito de negócio esperado, não falha técnica. Login ou CSRF falho invalida a rodada.

Meta inicial de desempenho: p95 de `Reserva concorrente` < 2000 ms, sem 5xx. Essa meta é proposta para laboratório e deve ser validada com o professor. Sem resultado medido nesta entrega.

## Preparação e execução

Instale JMeter 5.6.3 e Python 3. Suba a aplicação com banco descartável. Defina `ADMIN_PASSWORD` no ambiente; opcionalmente `BASE_URL` e `LOAD_USER` (conta ADMIN para criar o recurso).

```sh
python load/prepare.py
jmeter -n -t load/reservas.jmx -q load/local.properties -Jthreads=50 -l load/results.jtl -e -o load/results/html
python load/summarize.py load/results.jtl
```

Crie uma nova preparação para cada rodada: o recurso da rodada anterior já terá reserva. Use arquivos de saída novos ou remova saídas de uma rodada descartável antes de repetir. O arquivo `load/local.properties` contém senha, está no .gitignore e deve ser removido após o teste. Não anexe esse arquivo ao relatório.

O resumo retorna código 1 se a quantidade aceita/conflitante não for a esperada. Verifique também o HTML completo: samplers de login e token devem passar. Confirme a quantidade persistida no relatório da aplicação ou por consulta ao banco de teste, sem confiar somente no HTTP.

## Registrar por execução

- Data, commit, SO, versão Java, CPU/RAM, Docker e local do gerador de carga.
- Threads, ramp-up (5 s), barreira (até 30 s), duração, quantidade de pedidos.
- Throughput, p50/p95/p99, códigos HTTP e erros de conexão/servidor.
- Banco: número de reservas do recurso alvo; exatamente 1.
- Contenção observada e comparação entre 10, 50 e 100 threads, cada rodada com recurso novo.

## Resultado

**Executado em 05/11/2026 pelo job `load` do CI: aprovado nas três rodadas** (10, 50 e 100 usuários; 1 reserva aceita por rodada; p95 até 487 ms). Detalhes, ambiente e limitações em `docs/performance/resultado-01.md`. O job `load` em `.github/workflows/ci.yml` repete o teste a cada execução do CI.
