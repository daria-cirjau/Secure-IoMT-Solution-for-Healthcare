package com.disertatie;

import android.util.Log;
import com.disertatie.fhir.ChartImageLoader;
import com.disertatie.fhir.FhirObservationHandler;
import com.rabbitmq.client.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class RabbitMQSubscriber {

    private static final String QUEUE_NAME = "fhir_observations";
    private final FhirObservationHandler fhirObservationHandler;
    private final ChartImageLoader imageLoader;
    private boolean hasReceivedFirstMessage = false;
    private static final String BASE_URL = "https://10.0.2.2:8443/api/decrypt";

    public RabbitMQSubscriber(FhirObservationHandler fhirObservationHandler, ChartImageLoader imageLoader) {
        this.fhirObservationHandler = fhirObservationHandler;
        this.imageLoader = imageLoader;
    }

    public void start() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("10.0.2.2");
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.basicConsume(QUEUE_NAME, true, this::handleMessage, consumerTag -> {});
            imageLoader.showNoDataMessage(true);
            Log.d("RabbitMQ", "Started consuming...");
        } catch (Exception e) {
            Log.e("RabbitMQ", "Connection failed", e);
        }
    }

    private void handleMessage(String consumerTag, Delivery delivery) {
        try {
            byte[] encryptedCose = delivery.getBody();
            String decryptedJson = decryptMessage(encryptedCose);

            if (decryptedJson != null) {
                String imagePath = fhirObservationHandler.processObservation(decryptedJson);
                imageLoader.loadImage(imagePath);
            }

            if (!hasReceivedFirstMessage) {
                hasReceivedFirstMessage = true;
                imageLoader.showNoDataMessage(false);
            }

        } catch (Exception e) {
            Log.e("RabbitMQ", "Failed to process FHIR observation", e);
        }
    }

    private String decryptMessage(byte[] binaryPayload) {
        try {
            URL url = new URL(BASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(binaryPayload);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStream is = conn.getInputStream();
                     Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
                    return scanner.hasNext() ? scanner.next() : null;
                }
            } else {
                Log.e("HTTP", "Decryption failed with HTTP " + responseCode);
            }
        } catch (Exception e) {
            Log.e("HTTP", "Exception in decryptOverHttp", e);
        }
        return null;
    }
}
