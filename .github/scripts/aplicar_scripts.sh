#!/usr/bin/env bash
# Aplica, em ordem, os scripts de database/oracle em uma conexao Oracle.
# Uso: aplicar_scripts.sh <usuario> <senha> <origem> <script.sql> [script.sql ...]
#   origem no formato //host:porta/servico (ex.: //localhost:1521/FREEPDB1)
# Cada script ja possui WHENEVER SQLERROR EXIT FAILURE: um erro aborta o job.
set -euo pipefail

usuario="$1"; senha="$2"; origem="$3"; shift 3

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "$raiz/logs/banco"
cd "$raiz"

echo "## Scripts aplicados no Oracle" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
echo "| Script | Resultado |" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
echo "| --- | --- |" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

falhou=0
for script in "$@"; do
  caminho="database/oracle/$script"
  log="logs/banco/${script%.sql}.log"
  echo "::group::$script"
  if sql -S "${usuario}/${senha}@${origem}" "@${caminho}" 2>&1 | tee "$log"; then
    echo "| \`$script\` | OK |" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
  else
    echo "| \`$script\` | FALHOU |" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    falhou=1
  fi
  echo "::endgroup::"
  if [ "$falhou" -eq 1 ]; then
    break
  fi
done

exit "$falhou"
