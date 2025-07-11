#!/bin/bash

LOG_PATH=/var/log/mongodb.log
CERT_DIR=/etc/mongo/certs
CONF=/etc/mongo/mongod.conf

mkdir -p "$(dirname "$LOG_PATH")"
mongod --config "$CONF" --fork --logpath "$LOG_PATH"

until mongosh --tls \
       --host mongo \
       --tlsCertificateKeyFile "$CERT_DIR/mongo.pem" \
       --tlsCAFile "$CERT_DIR/certificate-authority.pem" \
       --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
  echo "Waiting for MongoDB to start..."
  sleep 2
done

USER=$(< /run/secrets/mongo_user)
PASS=$(< /run/secrets/mongo_pass)

mongosh --tls \
  --host mongo \
  --tlsCertificateKeyFile "$CERT_DIR/mongo.pem" \
  --tlsCAFile "$CERT_DIR/certificate-authority.pem" \
  --authenticationDatabase admin \
  --eval "
    const admin = db.getSiblingDB('admin');
    if (!admin.getUser('$USER')) {
      admin.createUser({
        user: '$USER',
        pwd: '$PASS',
        roles: [ { role: 'root', db: 'admin' } ]
      });
      print('MongoDB user \"$USER\" created.');
    } else {
      print('ℹMongoDB user \"$USER\" already exists.');
    }
  "

# Keep container running
exec tail -f "$LOG_PATH"
