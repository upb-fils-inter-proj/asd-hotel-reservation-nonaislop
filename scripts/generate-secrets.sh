#!/usr/bin/env bash
# Creates random API passwords in ./secrets for docker compose. Existing secrets are kept.
# The secrets/ folder is git-ignored: never commit it.
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p secrets
chmod 700 secrets

for name in hotel_api_password shop_api_password payment_api_password admin_password \
            hotel_db_password shop_db_password payment_db_password; do
  file="secrets/$name.txt"
  if [ -s "$file" ]; then
    echo "kept    $file"
    continue
  fi
  # No trailing newline; readable inside the containers, which run as a non-root user.
  (umask 022; head -c 32 /dev/urandom | base64 | tr -d '\n/+=' | head -c 32 > "$file")
  echo "created $file"
done
