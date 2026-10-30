#!/usr/bin/env python3
"""Resume os relatorios do Surefire em Markdown.

Uso: relatorio_testes.py "<titulo da etapa>"
Escreve em logs/relatorio-testes.md e no resumo do job no GitHub Actions.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

titulo = sys.argv[1] if len(sys.argv) > 1 else "Testes automatizados"

linhas = []
total = falhas = erros = ignorados = 0
tempo = 0.0

for arquivo in sorted(glob.glob("target/surefire-reports/*.xml")):
    try:
        raiz = ET.parse(arquivo).getroot()
    except ET.ParseError:
        continue
    if raiz.tag != "testsuite":
        continue
    nome = raiz.get("name", os.path.basename(arquivo))
    t = int(raiz.get("tests", 0))
    f = int(raiz.get("failures", 0))
    e = int(raiz.get("errors", 0))
    s = int(raiz.get("skipped", 0))
    d = float(raiz.get("time", 0) or 0)
    total, falhas, erros, ignorados, tempo = total + t, falhas + f, erros + e, ignorados + s, tempo + d
    situacao = "FALHOU" if (f or e) else ("IGNORADA" if s == t and t else "OK")
    linhas.append(f"| `{nome.split('.')[-1]}` | {t} | {f + e} | {s} | {d:.2f}s | {situacao} |")

passaram = total - falhas - erros - ignorados
cabecalho = [
    f"## {titulo}",
    "",
    f"**{passaram} aprovados**, {falhas + erros} com falha, {ignorados} ignorados "
    f"de {total} testes em {tempo:.2f}s.",
    "",
    "| Classe de teste | Testes | Falhas | Ignorados | Tempo | Situacao |",
    "| --- | ---: | ---: | ---: | ---: | --- |",
]
relatorio = "\n".join(cabecalho + (linhas or ["| _nenhum relatorio encontrado_ | - | - | - | - | - |"])) + "\n"

os.makedirs("logs", exist_ok=True)
with open("logs/relatorio-testes.md", "w", encoding="utf-8") as saida:
    saida.write(relatorio)

resumo = os.environ.get("GITHUB_STEP_SUMMARY")
if resumo:
    with open(resumo, "a", encoding="utf-8") as saida:
        saida.write(relatorio)

print(relatorio)
