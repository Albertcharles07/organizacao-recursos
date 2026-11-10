"""Resume os relatórios do Surefire, Failsafe e JaCoCo no GitHub Actions.

Gera uma anotação de erro por teste que falhou (aparece no PR), uma anotação com o total de
testes e de cobertura, e uma tabela no resumo do job. Nunca falha o job: quem decide se o build
passou é o próprio Maven.
"""
import csv
import glob
import os
import xml.etree.ElementTree as ET

totais = {"testes": 0, "falhas": 0, "erros": 0, "ignorados": 0}
por_classe = []
falhos = []

for arquivo in sorted(glob.glob("target/surefire-reports/TEST-*.xml") + glob.glob("target/failsafe-reports/TEST-*.xml")):
    suite = ET.parse(arquivo).getroot()
    nome = suite.get("name", "").split(".")[-1]
    t, f, e, s = (int(suite.get(k, 0)) for k in ("tests", "failures", "errors", "skipped"))
    por_classe.append((nome, t, f + e, s))
    for chave, valor in zip(totais, (t, f, e, s)):
        totais[chave] += valor
    for caso in suite.iter("testcase"):
        problema = caso.find("failure")
        if problema is None:
            problema = caso.find("error")
        if problema is not None:
            mensagem = (problema.get("message") or problema.get("type") or "").replace("\n", " ")[:300]
            falhos.append((f"{nome}.{caso.get('name')}", mensagem))

cobertura = ""
csv_jacoco = "target/site/jacoco/jacoco.csv"
if os.path.exists(csv_jacoco):
    linhas = list(csv.DictReader(open(csv_jacoco)))
    soma = lambda k: sum(int(l[k]) for l in linhas)
    lc, lm, bc, bm = soma("LINE_COVERED"), soma("LINE_MISSED"), soma("BRANCH_COVERED"), soma("BRANCH_MISSED")
    cobertura = f"linhas {lc / (lc + lm):.2%} ({lc}/{lc + lm}), branches {bc / (bc + bm):.2%} ({bc}/{bc + bm})"

for teste, mensagem in falhos:
    print(f"::error title=Teste falhou: {teste}::{mensagem}")
resumo = f"{totais['testes']} testes, {totais['falhas']} falhas, {totais['erros']} erros, {totais['ignorados']} ignorados"
if cobertura:
    resumo += f"; cobertura: {cobertura}"
print(f"::notice title=Resumo dos testes::{resumo}")

saida = os.environ.get("GITHUB_STEP_SUMMARY")
if saida:
    with open(saida, "a", encoding="utf-8") as md:
        md.write("## Resumo dos testes\n\n" + resumo + "\n\n| Classe | Testes | Falhas e erros | Ignorados |\n| --- | --- | --- | --- |\n")
        for nome, t, fe, s in por_classe:
            md.write(f"| {nome} | {t} | {fe} | {s} |\n")
        if falhos:
            md.write("\n### Testes que falharam\n\n" + "".join(f"- `{t}`: {m}\n" for t, m in falhos))
