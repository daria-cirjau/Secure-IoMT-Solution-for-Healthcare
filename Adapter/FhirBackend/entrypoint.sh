#!/bin/sh
set -e

echo "---- Dumping Mongo Secrets ----"
echo "mongo_user: $(cat /run/secrets/mongo_user 2>/dev/null || echo 'MISSING')"
echo "truststore_password: $(cat /run/secrets/truststore_password 2>/dev/null || echo 'MISSING')"

echo "---- Running Backend ----"
exec java \
  -Dmongo.user="$(cat /run/secrets/mongo_user)" \
  -Dmongo.pass="$(cat /run/secrets/mongo_pass)" \
  -Djavax.net.ssl.trustStore=/app/certs/mongodb-truststore.jks \
  -Djavax.net.ssl.trustStorePassword="$(cat /run/secrets/truststore_password)" \
  -jar fhirbackend.jar
