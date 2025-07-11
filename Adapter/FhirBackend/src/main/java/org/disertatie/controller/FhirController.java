package org.disertatie.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import org.bson.Document;
import org.disertatie.service.FhirService;

import java.util.List;
import java.util.Map;

@Controller("/api")
public class FhirController {

    @Inject
    FhirService fhirService;

    @Post("/login")
    public Map<String, Object> login(@Body Map<String, String> credentials) {
        return fhirService.authenticate(credentials);
    }

    @Get("/patients")
    public List<Map<String, String>> getPatients(@QueryValue String doctorId) {
        return fhirService.getPatientsForDoctor(doctorId);
    }

    @Get("/encounters")
    public List<Document> getEncounters(@QueryValue String patientId) {
        return fhirService.getEncountersForPatient(patientId);
    }

    @Get("/observations")
    public List<JsonNode> getObservations(@QueryValue String patientId,
                                          @QueryValue String dateAfter,
                                          @QueryValue String dateBefore) {
        return fhirService.getObservationsForPatient(patientId, dateAfter, dateBefore);
    }

    @Post("/decrypt")
    @Consumes("application/octet-stream")
    @Produces("application/json")
    public JsonNode decryptFhir(@Body byte[] fhirBytes) throws Exception {
        return fhirService.decryptObservation(fhirBytes);
    }

}
