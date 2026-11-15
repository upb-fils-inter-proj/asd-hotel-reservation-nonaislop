#!/usr/bin/env bash
# Writes postman/hotel-reservation-local.with-secrets.postman_environment.json: the Postman environment
# with the passwords from ./secrets filled in. That file is git-ignored; never commit it.
set -euo pipefail
cd "$(dirname "$0")/.."

template=postman/hotel-reservation-local.postman_environment.json
target=postman/hotel-reservation-local.with-secrets.postman_environment.json

for name in hotel_api_password shop_api_password payment_api_password admin_password; do
  [ -s "secrets/$name.txt" ] || { echo "secrets/$name.txt is missing: run ./scripts/generate-secrets.sh first" >&2; exit 1; }
done

python3 -I - "$template" "$target" <<'PY'
import json, sys
template, target = sys.argv[1], sys.argv[2]
secrets = {
    "hotelApiPassword": "hotel_api_password",
    "shopApiPassword": "shop_api_password",
    "paymentApiPassword": "payment_api_password",
    "adminPassword": "admin_password",
}
env = json.load(open(template))
env["name"] += " + secrets"
for value in env["values"]:
    if value["key"] in secrets:
        value["value"] = open(f"secrets/{secrets[value['key']]}.txt").read().strip()
with open(target, "w") as out:
    json.dump(env, out, indent=2)
PY
chmod 600 "$target"
echo "wrote $target (git-ignored)"
