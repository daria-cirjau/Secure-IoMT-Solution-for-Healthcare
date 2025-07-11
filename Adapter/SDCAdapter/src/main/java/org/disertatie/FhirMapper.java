package org.disertatie;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.disertatie.FhirCryptoUtil;
import org.hl7.fhir.r5.model.*;

import java.util.Date;

public class FhirMapper {
    private static final FhirContext ctx = FhirContext.forR5();
    private final MongoWriter mongoWriter;
    private final RabbitMQPublisher rabbit;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FhirMapper() {
        this.mongoWriter = new MongoWriter();
        try {
            this.rabbit = new RabbitMQPublisher();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createObservation(
            double value,
            String loincCode,
            String loincDisplay,
            String unit,
            String unitCode,
            String patientId,
            String deviceId
    ) {
        Observation returnedObservation = new Observation();
        returnedObservation.setStatus(Enumerations.ObservationStatus.FINAL);
        Date timestamp = new Date();
        returnedObservation.setEffective(new DateTimeType(timestamp));
        returnedObservation.setSubject(new Reference("Patient/" + patientId));
        if (deviceId != null && !deviceId.isBlank()) {
            returnedObservation.setDevice(new Reference("Device/" + deviceId));
        }
        returnedObservation.setCode(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode(loincCode)
                .setDisplay(loincDisplay)));
        returnedObservation.setValue(new Quantity()
                .setValue(value)
                .setUnit(unit)
                .setSystem("http://unitsofmeasure.org")
                .setCode(unitCode));

        String json = ctx.newJsonParser().encodeResourceToString(returnedObservation);

        try {
            JsonNode node = objectMapper.readTree(json);
            byte[] encrypted = FhirCryptoUtil.encryptFhirJson(node);

            rabbit.publish(encrypted);
            System.out.println("Published encrypted observation to RabbitMQ.");

            mongoWriter.saveObservation(encrypted, timestamp, patientId);
            System.out.println("Saved observation to MongoDB: " + json);

        } catch (Exception e) {
            System.err.println("Failed to process and encrypt observation.");
            e.printStackTrace();
        }
    }
}
