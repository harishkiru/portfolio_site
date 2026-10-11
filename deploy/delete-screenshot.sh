#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
image_id=${1:-}
[[ "$image_id" =~ ^[A-Za-z0-9_-]{32}$ ]] || { echo 'Provide the 32-character image ID.' >&2; exit 1; }
docker compose exec -T portfolio sh -c '
    printf "header = \"Authorization: Bearer %s\"\n" "$(cat /run/secrets/sharex.upload-token)" |
    curl --config - --fail --silent --show-error --request DELETE \
        "http://127.0.0.1:8080/api/sharex/$1"
' sh "$image_id"
echo 'Screenshot deleted.'
