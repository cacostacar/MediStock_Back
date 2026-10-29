#!/usr/bin/env bash
# Faz o rollout da imagem publicada no GHCR em um servidor acessivel por SSH.
# Variaveis esperadas: SSH_HOST, SSH_USER, SSH_KEY, SSH_PORT, IMAGEM,
#                      GHCR_TOKEN, ORACLE_URL, ORACLE_USER, ORACLE_PASSWORD,
#                      JWT_SECRET, GEMINI_API_KEY
set -euo pipefail

mkdir -p logs ~/.ssh
printf '%s\n' "$SSH_KEY" > ~/.ssh/deploy_key
chmod 600 ~/.ssh/deploy_key
ssh-keyscan -p "$SSH_PORT" -H "$SSH_HOST" >> ~/.ssh/known_hosts 2>/dev/null

# O script remoto e montado aqui e enviado pela entrada padrao, para que as
# credenciais nao aparecam na linha de comando do servidor.
remoto=$(cat <<REMOTO
set -euo pipefail
printf '%s' $(printf '%q' "$GHCR_TOKEN") | docker login ghcr.io -u $(printf '%q' "$GITHUB_ACTOR") --password-stdin
docker pull $(printf '%q' "$IMAGEM")
docker rm -f medistock-backend 2>/dev/null || true
docker run -d --name medistock-backend --restart unless-stopped \\
  -p 8080:8080 \\
  -e SPRING_PROFILES_ACTIVE=oracle \\
  -e ORACLE_URL=$(printf '%q' "$ORACLE_URL") \\
  -e ORACLE_USER=$(printf '%q' "$ORACLE_USER") \\
  -e ORACLE_PASSWORD=$(printf '%q' "$ORACLE_PASSWORD") \\
  -e JWT_SECRET=$(printf '%q' "$JWT_SECRET") \\
  -e GEMINI_API_KEY=$(printf '%q' "${GEMINI_API_KEY:-}") \\
  $(printf '%q' "$IMAGEM")
docker image prune -f
docker ps --filter name=medistock-backend
REMOTO
)

set -o pipefail
printf '%s\n' "$remoto" |
  ssh -i ~/.ssh/deploy_key -p "$SSH_PORT" "${SSH_USER}@${SSH_HOST}" 'bash -s' 2>&1 |
  tee logs/rollout.log
