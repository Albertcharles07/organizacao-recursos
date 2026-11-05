"""Resume as rodadas de carga do JMeter e falha o job se alguma rodada for inconsistente.

Para cada rodada (10, 50 e 100 usuários) confere: exatamente 1 resposta 200, todas as outras
409, nenhum outro código, e exatamente 1 alocação gravada no banco para o recurso disputado.
"""
import json
import os
import pathlib
import sys

pasta = pathlib.Path("load/results")
linhas = []
ok = True
for threads in (10, 50, 100):
    resumo_json = pasta / f"resumo-{threads}.json"
    banco_txt = pasta / f"banco-{threads}.txt"
    if not resumo_json.exists():
        print(f"::error title=Carga {threads} usuários::rodada não gerou resultado")
        ok = False
        continue
    r = json.loads(resumo_json.read_text())
    no_banco = int(banco_txt.read_text().strip() or -1) if banco_txt.exists() else -1
    passou = r["consistency_pass"] and r["other"] == 0 and no_banco == 1
    ok &= passou
    texto = (
        f"{threads} usuários: {r['requests']} pedidos, {r['accepted']} aceitos (200), "
        f"{r['conflicts']} conflitos (409), {r['other']} outros, p95 {r['p95_ms']} ms, "
        f"{no_banco} reserva(s) no banco: {'OK' if passou else 'FALHOU'}"
    )
    print(f"::{'notice' if passou else 'error'} title=Carga {threads} usuários::{texto}")
    linhas.append((threads, r, no_banco, passou))

ambiente = (pasta / "ambiente.txt").read_text().strip().replace("\n", "; ") if (pasta / "ambiente.txt").exists() else ""
if ambiente:
    print(f"::notice title=Ambiente da carga::{ambiente}")

saida = os.environ.get("GITHUB_STEP_SUMMARY")
if saida:
    with open(saida, "a", encoding="utf-8") as md:
        md.write("## Teste de carga (JMeter)\n\n| Usuários | Pedidos | 200 | 409 | Outros | p95 (ms) | Reservas no banco | Resultado |\n")
        md.write("| --- | --- | --- | --- | --- | --- | --- | --- |\n")
        for threads, r, no_banco, passou in linhas:
            md.write(f"| {threads} | {r['requests']} | {r['accepted']} | {r['conflicts']} | {r['other']} | {r['p95_ms']} | {no_banco} | {'OK' if passou else 'FALHOU'} |\n")
        md.write(f"\nAmbiente: {ambiente}\n")

sys.exit(0 if ok and len(linhas) == 3 else 1)
