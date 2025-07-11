package org.disertatie;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQPublisher {
    private static final String QUEUE_NAME = "fhir_observations";
    private static final String HOST = "rabbitmq";

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    public RabbitMQPublisher() {
        setupConnection();
    }

    private void setupConnection() {
        try {
            factory = new ConnectionFactory();
            factory.setHost(HOST);
            factory.setPort(5672);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);
            factory.setRequestedHeartbeat(30);

            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.confirmSelect();

            System.out.println("[RabbitMQ] Connected and ready.");
        } catch (Exception e) {
            System.err.println("[RabbitMQ] Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void publish(byte[] message) {
        try {
            ensureConnection();

            channel.basicPublish("", QUEUE_NAME, null, message);
            channel.waitForConfirmsOrDie(5000);

            System.out.println("[RabbitMQ] Message published.");
        } catch (Exception e) {
            System.err.println("[RabbitMQ] Publish failed: " + e.getMessage());
            e.printStackTrace();
            tryReconnect();
        }
    }

    private void ensureConnection() throws IOException, TimeoutException {
        if (connection == null || !connection.isOpen()) {
            System.out.println("[RabbitMQ] Reconnecting connection...");
            setupConnection();
        } else if (channel == null || !channel.isOpen()) {
            System.out.println("[RabbitMQ] Reconnecting channel...");
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.confirmSelect();
        }
    }

    private void tryReconnect() {
        close();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}
        setupConnection();
    }

    public void close() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
        } catch (Exception ignored) {}

        try {
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception ignored) {}

        System.out.println("[RabbitMQ] Connection closed.");
    }
}
