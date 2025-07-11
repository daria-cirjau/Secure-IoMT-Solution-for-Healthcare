package org.disertatie;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

public class MqttConnectionManager {

    public static MqttConnectOptions buildOptions() throws Exception {
        String keystorePath = System.getProperty("javax.net.ssl.keyStore");
        String keystorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
        String truststorePath = System.getProperty("javax.net.ssl.trustStore");
        String truststorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(configureSSLSocketFactory(
            keystorePath, keystorePassword, truststorePath, truststorePassword
        ));

        return options;
    }

    private static SSLSocketFactory configureSSLSocketFactory(
        String keystorePath, String keystorePassword,
        String truststorePath, String truststorePassword
    ) throws Exception {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(truststorePath), truststorePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keystorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }
}
