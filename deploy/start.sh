#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compose_files=(-f compose.yaml)
umask 077
for secret in r2.access-key-id r2.secret-access-key; do
    if [[ ! -f "secrets/$secret" ]]; then : > "secrets/$secret"; fi
done
if [[ ! -f secrets/sharex.upload-token ]]; then
    umask 077
    command -v openssl >/dev/null || { echo 'Install openssl first.' >&2; exit 1; }
    openssl rand -hex 32 | tr -d '\n' > secrets/sharex.upload-token
fi
if [[ "${1:-}" == "--tunnel" ]]; then
    compose_files+=(-f compose.tunnel.yaml)
    [[ -s secrets/cloudflare.tunnel-token ]] || { echo 'Save the tunnel token first.' >&2; exit 1; }
elif [[ -n "${1:-}" ]]; then
    echo 'Usage: bash deploy/start.sh [--tunnel]' >&2
    exit 1
fi
docker compose "${compose_files[@]}" config --quiet
docker compose "${compose_files[@]}" build portfolio
app_image=$(docker compose "${compose_files[@]}" config --images portfolio)
docker run --rm --user 0 --entrypoint sh \
    --mount "type=bind,src=$PWD/secrets,dst=/secure" "$app_image" \
    -c 'chown 10001:10001 /secure/owner.password /secure/lastfm.api-key /secure/sharex.upload-token /secure/r2.access-key-id /secure/r2.secret-access-key && chmod 600 /secure/owner.password /secure/lastfm.api-key /secure/sharex.upload-token /secure/r2.access-key-id /secure/r2.secret-access-key'
if [[ "${1:-}" == "--tunnel" ]]; then
    docker run --rm --user 0 --entrypoint sh \
        --mount "type=bind,src=$PWD/secrets,dst=/secure" "$app_image" \
        -c 'chown 65532:65532 /secure/cloudflare.tunnel-token && chmod 600 /secure/cloudflare.tunnel-token'
fi
docker compose "${compose_files[@]}" up -d --no-build --wait --wait-timeout 120
