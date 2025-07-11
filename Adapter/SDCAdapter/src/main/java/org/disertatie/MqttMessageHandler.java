package org.disertatie;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttMessageHandler implements MqttCallback {

    private final FhirMapper fhirMapper;

    public MqttMessageHandler() {
        this.fhirMapper = new FhirMapper();
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            byte[] cosePayload = message.getPayload();

            JsonNode decryptedJson = FhirCryptoUtil.decryptCose(cosePayload);

            String patientId = "1";
            String deviceOid = decryptedJson.get("device_oid").asText();
            JsonNode metrics = decryptedJson.get("metrics");

            switch (topic) {
                case "sensors/ekg":
                    if (metrics.has("ekg")) {
                        double ekg = metrics.get("ekg").asDouble();
                        fhirMapper.createObservation(ekg, "11524-6", "EKG study", "V", "V", patientId, deviceOid);
                    }
                    break;

                case "sensors/temperature":
                    if (metrics.has("temperature")) {
                        double temp = metrics.get("temperature").asDouble();
                        fhirMapper.createObservation(temp, "8310-5", "Body temperature", "Cel", "Cel", patientId, deviceOid);
                    }
                    break;

                case "sensors/pulse_oximetry":
                    if (metrics.has("oxygen")) {
                        double oxygen = metrics.get("oxygen").asDouble();
                        fhirMapper.createObservation(oxygen, "59408-5", "Oxygen saturation", "%", "%", patientId, deviceOid);
                    }
                    if (metrics.has("pulse")) {
                        double pulse = metrics.get("pulse").asDouble();
                        fhirMapper.createObservation(pulse, "8867-4", "Heart rate", "beats/min", "beats/min", patientId, deviceOid);
                    }
                    break;
            }

        } catch (Exception e) {
            System.err.println("Error processing MQTT message:");
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
