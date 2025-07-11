FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/fhirbackend-*.jar fhirbackend.jar

COPY entrypoint.sh .
COPY certs/mongo-certs/certificate-authority.pem ./certs/
COPY certs/mongo-certs/mongodb-truststore.jks ./certs/
COPY certs/android/keystore.p12 ./certs/keystore.p12

RUN chmod +x entrypoint.sh

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]
