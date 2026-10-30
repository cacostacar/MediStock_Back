#!/usr/bin/env python3
"""Consolida o relatorio final da execucao do pipeline.

Le os artefatos baixados em artefatos/ e o resultado de cada job nas
variaveis STATUS_BUILD, STATUS_ORACLE e STATUS_DEPLOY.
"""
import datetime
import os
import pathlib

SIMBOLOS = {"success": "OK", "failure": "FALHOU", "cancelled": "CANCELADO", "skipped": "NAO EXECUTADO"}


def situacao(valor):
    return SIMBOLOS.get(valor, valor or "-")


repo = os.environ.get("GITHUB_REPOSITORY", "")
execucao = os.environ.get("GITHUB_RUN_ID", "")
partes = [
    "# Relatorio de execucao do pipeline - MediStock (Smart HAS)",
    "",
    f"- **Repositorio:** {repo}",
    f"- **Commit:** {os.environ.get('GITHUB_SHA', '')}",
    f"- **Referencia:** {os.environ.get('GITHUB_REF_NAME', '')}",
    f"- **Evento:** {os.environ.get('GITHUB_EVENT_NAME', '')}",
    f"- **Execucao:** https://github.com/{repo}/actions/runs/{execucao}",
    f"- **Gerado em:** {datetime.datetime.now(datetime.timezone.utc):%Y-%m-%d %H:%M:%S} UTC",
    "",
    "## Situacao dos jobs",
    "",
    "| Etapa | Resultado |",
    "| --- | --- |",
    f"| Build e testes | {situacao(os.environ.get('STATUS_BUILD'))} |",
    f"| Integracao Oracle (PL/SQL) | {situacao(os.environ.get('STATUS_ORACLE'))} |",
    f"| Deploy (back-end + Oracle) | {situacao(os.environ.get('STATUS_DEPLOY'))} |",
    "",
]

for rotulo, caminho in [
    ("Testes do build", "artefatos/relatorios-build/logs/relatorio-testes.md"),
    ("Testes de integracao com o Oracle", "artefatos/relatorios-oracle/logs/relatorio-testes.md"),
]:
    arquivo = pathlib.Path(caminho)
    if arquivo.is_file():
        partes += [arquivo.read_text(encoding="utf-8"), ""]

logs = sorted(p for p in pathlib.Path("artefatos").rglob("*.log")) if pathlib.Path("artefatos").exists() else []
if logs:
    partes += ["## Logs publicados como artefatos", ""]
    partes += [f"- `{p.relative_to('artefatos')}` ({p.stat().st_size} bytes)" for p in logs]
    partes.append("")

relatorio = "\n".join(partes)
pathlib.Path("relatorio-execucao.md").write_text(relatorio, encoding="utf-8")

resumo = os.environ.get("GITHUB_STEP_SUMMARY")
if resumo:
    with open(resumo, "a", encoding="utf-8") as saida:
        saida.write(relatorio)

print(relatorio)
