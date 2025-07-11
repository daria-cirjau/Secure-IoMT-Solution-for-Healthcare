#!/bin/sh
set -e

# Detect default gateway IP (host IP on Docker bridge)
HOST_IP=$(ip route | awk '/default/ { print $3 }')

echo "Detected host IP: $HOST_IP"

# Add host entry to /etc/hosts
echo "$HOST_IP daria-laptop.local" >> /etc/hosts

echo "---- Dumping secrets ----"
ls -l /run/secrets || echo "No secrets found"
echo "mongo_user: $(cat /run/secrets/mongo_user 2>/dev/null || echo 'MISSING')"
echo "keystore_password: $(cat /run/secrets/keystore_password 2>/dev/null || echo 'MISSING')"

echo "---- Checking keystore files ----"
ls -l /app/certs || echo "/app/certs missing"

echo "---- Running Java ----"
exec java \
  -Djavax.net.ssl.keyStore=/app/certs/client.keystore.jks \
  -Djavax.net.ssl.keyStorePassword="$(cat /run/secrets/keystore_password)" \
  -Djavax.net.ssl.trustStore=/app/certs/client.truststore.jks \
  -Djavax.net.ssl.trustStorePassword="$(cat /run/secrets/truststore_password)" \
  -Dmongo.user="$(cat /run/secrets/mongo_user)" \
  -Dmongo.pass="$(cat /run/secrets/mongo_pass)" \
  -jar SDCAdapter.jar
