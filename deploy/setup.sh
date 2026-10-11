#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p secrets
chmod 700 secrets
if [[ ! -f .env ]]; then cp .env.example .env; fi
if [[ ! -f secrets/owner.password ]]; then
    command -v openssl >/dev/null || { echo 'Install openssl first.' >&2; exit 1; }
    openssl rand -hex 24 | tr -d '\n' > secrets/owner.password
    echo 'Generated a unique owner password in secrets/owner.password. Save it in your password manager.'
fi
if [[ ! -f secrets/lastfm.api-key ]]; then
    read -r -s -p 'Last.fm API key (Enter to leave music unavailable): ' api_key
    printf '\n'
    printf '%s' "$api_key" > secrets/lastfm.api-key
    unset api_key
fi
chmod 644 secrets/owner.password secrets/lastfm.api-key
echo 'Setup complete. Review .env, then follow server/DEPLOYMENT.md.'
