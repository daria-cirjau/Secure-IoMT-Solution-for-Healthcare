package org.disertatie.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.inject.Singleton;

@Singleton
public class MongoConnector {

    private final MongoClient client;

    public MongoConnector() {
        String username = System.getProperty("mongo.user");
        String password = System.getProperty("mongo.pass");

        if (username == null || password == null) {
            throw new RuntimeException("Missing MongoDB credentials");
        }

        String connectionString = String.format(
                "mongodb://%s:%s@mongo:27017/?tls=true&tlsCAFile=/app/certs/certificate-authority.pem&authSource=admin",
                username, password
        );

        client = MongoClients.create(connectionString);
    }

    public MongoDatabase getDatabase() {
        return client.getDatabase("fhirDB");
    }

    public MongoClient getClient() {
        return client;
    }
}
