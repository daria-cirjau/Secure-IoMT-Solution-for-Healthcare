package com.disertatie.fhir;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;

import java.util.List;

import ca.uhn.fhir.context.FhirContext;

public class FhirObservationHandler {

    private final Python py;
    private final PyObject chartModule;
    private final FhirContext fhirContext;

    public FhirObservationHandler() {
        py = Python.getInstance();
        chartModule = py.getModule("chart_manager");
        fhirContext = FhirContext.forR4();
    }

    public String processObservation(String obsJson) {
        Observation obs = (Observation) fhirContext.newJsonParser().parseResource(obsJson);

        String code = obs.getCode().getCodingFirstRep().getCode();
        Quantity quantity = obs.getValueQuantity();
        if (quantity == null) return "";

        double value = quantity.getValue().doubleValue();

        switch (code) {
            case "11524-6":
                chartModule.callAttr("add_ekg", value);
                break;
            case "8310-5":
                chartModule.callAttr("add_temp", value);
                break;
            case "59408-5":
                chartModule.callAttr("addOxygen", value);
                break;
            case "8867-4":
                chartModule.callAttr("addPulse", value);
                break;

        }

        PyObject result = chartModule.callAttr("render");
        return result.toString();
    }
}
