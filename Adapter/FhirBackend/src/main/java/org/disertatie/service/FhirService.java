package org.disertatie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Filters;
import org.bson.types.Binary;
import org.disertatie.mongo.MongoConnector;

import java.time.Instant;
import java.util.*;

@Singleton
public class FhirService {

    private final MongoCollection<Document> users;
    private final MongoCollection<Document> patients;
    private final MongoCollection<Document> encounters;
    private final MongoCollection<Document> observations;
    private final MongoCollection<Document> practitioner;

    public FhirService(MongoConnector mongoConnector) {
        MongoDatabase db = mongoConnector.getDatabase();
        this.users = db.getCollection("users");
        this.patients = db.getCollection("Patient");
        this.encounters = db.getCollection("Encounter");
        this.observations = db.getCollection("observations");
        this.practitioner = db.getCollection("Practitioner");
    }

    public Map<String, Object> authenticate(Map<String, String> credentials) {
        String user = credentials.get("username");
        String pass = credentials.get("password");
        Document match = users.find(new Document("username", user).append("password", pass)).first();
        Map<String, Object> result = new HashMap<>();
        if (match != null) {
            result.put("success", true);
            result.put("practitionerId", match.getString("practitionerId"));
        } else {
            result.put("success", false);
        }
        return result;
    }

    public List<Map<String, String>> getPatientsForDoctor(String doctorId) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Document encounter : encounters.find(new Document("participant.individual.reference", "Practitioner/" + doctorId))) {
            Document subject = (Document) encounter.get("subject");
            String patientRef = subject.getString("reference");
            String patientId = patientRef.split("/")[1];

            Document patient = patients.find(new Document("id", patientId)).first();
            if (patient != null) {
                Document name = patient.getList("name", Document.class).get(0);
                String fullName = name.getList("given", String.class).get(0) + " " + name.getString("family");
                Map<String, String> patientInfo = new HashMap<>();
                patientInfo.put("id", patientId);
                patientInfo.put("name", fullName);
                list.add(patientInfo);
            }
        }
        return list;
    }

    public List<Document> getEncountersForPatient(String patientId) {
        return encounters.find(Filters.eq("subject.reference", "Patient/" + patientId)).into(new ArrayList<>());
    }

    public List<JsonNode> getObservationsForPatient(
            String patientId,
            String dateAfterIso,
            String dateBeforeIso
    ) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("patientId", patientId));

        Instant after = Instant.parse(dateAfterIso);
        filters.add(Filters.gte("timestamp", Date.from(after)));

        Instant before = Instant.parse(dateBeforeIso);
        filters.add(Filters.lte("timestamp", Date.from(before)));

        Bson combined = Filters.and(filters);
        List<Document> docs = observations.find(combined)
                .into(new ArrayList<>());

        List<JsonNode> result = new ArrayList<>();
        for (Document doc : docs) {
            try {
                byte[] cose = doc.get("encrypted", Binary.class).getData();
                JsonNode node = FhirCryptoUtil.decryptCose(cose);
                result.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public JsonNode decryptObservation(byte[] coseBytes) throws Exception {
        return FhirCryptoUtil.decryptCose(coseBytes);
    }
}
