package org.disertatie;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public class MqttSubscriber {

    private static final String BROKER_URI = "ssl://daria-laptop.local:8883";
    private static final String CLIENT_ID = "JavaSubscriberX509";

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER_URI, CLIENT_ID);
            MqttConnectOptions options = MqttConnectionManager.buildOptions();

            client.setCallback(new MqttMessageHandler());

            client.connect(options);
            System.out.println("Connected to broker.");

            client.subscribe("sensors/ekg");
            client.subscribe("sensors/temperature");
            client.subscribe("sensors/pulse_oximetry");
            System.out.println("Subscribed to topics.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Cause: " + e.getCause());
        }
    }
}
