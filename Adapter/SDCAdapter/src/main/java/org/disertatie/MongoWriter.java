package org.disertatie;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.hl7.fhir.r5.model.DateTimeType;

import java.util.Date;

public class MongoWriter {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoWriter() {
        String username = System.getProperty("mongo.user");
        String password = System.getProperty("mongo.pass");

        if (username == null || password == null) {
            throw new RuntimeException("Missing MongoDB credentials in system properties");
        }

        String connectionString = String.format(
                "mongodb://%s:%s@mongo:27017/?tls=true", username, password
        );

        client = MongoClients.create(connectionString);
        MongoDatabase database = client.getDatabase("fhirDB");
        collection = database.getCollection("observations");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
            System.out.println("MongoClient closed.");
        }));
    }

    public void saveObservation(byte[] encryptedFhirJson, Date timestamp, String patientId) {
        try {
            Document doc = new Document().append("encrypted", encryptedFhirJson).append("timestamp", timestamp).append("patientId", patientId);
            collection.insertOne(doc);
            System.out.println("Saved encrypted observation to MongoDB.");
        } catch (Exception e) {
            System.err.println("Failed to write observation to MongoDB:");
            e.printStackTrace();
        }
    }
}
