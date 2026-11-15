#!/bin/sh
# Builds the broker's password file from the Docker secrets, then starts Mosquitto.
# The plain-text passwords never leave /run/secrets; the file only holds their hashes.
set -eu
passwd=/mosquitto/data/passwd
umask 077
: > "$passwd"
for login in hotel-management:mqtt_hotel_password shop:mqtt_shop_password payment:mqtt_payment_password; do
  user=${login%%:*}
  secret=/run/secrets/${login#*:}
  if [ ! -s "$secret" ]; then
    echo "$secret is missing: run ./scripts/generate-secrets.sh first" >&2
    exit 1
  fi
  printf '%s:%s\n' "$user" "$(cat "$secret")" >> "$passwd"
done
mosquitto_passwd -U "$passwd"
chown mosquitto:mosquitto "$passwd"
exec mosquitto -c /mosquitto/config/mosquitto.conf
