FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/SDCAdapter-*-jar-with-dependencies.jar SDCAdapter.jar
COPY certs/client-keystores/ ./certs/
COPY entrypoint.sh .

RUN chmod +x entrypoint.sh

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]
