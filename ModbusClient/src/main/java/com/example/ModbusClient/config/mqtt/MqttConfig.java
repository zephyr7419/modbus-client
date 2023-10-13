package com.example.ModbusClient.config.mqtt;

import com.example.ModbusClient.util.mqtt.MqttPayloadMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MqttConfig {

    @Value("${mqtt.server-url}")
    private String serverUrl;
    @Value("${mqtt.ca-file}")
    private String caFilePath;
    @Value("${mqtt.client-crt-file}")
    private String clientCrtFilePath;
    @Value("${mqtt.client-key-file}")
    private String clientKeyFilePath;
    @Value("${mqtt.client-id}")
    private String clientId;
    @Value("${mqtt.application-id}")
    private String applicationId;

    private final MqttPayloadMap mqttPayloadMap;
    @Bean
    public MqttClient mqttClient() throws Exception {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        options.setCleanStart(false);
        options.setAutomaticReconnect(true);
        options.setSocketFactory(getSocketFactory(caFilePath, clientCrtFilePath, clientKeyFilePath, ""));


        MqttClient client = new MqttClient(serverUrl, clientId);
        String topic = "gasfan/+/event/control";

        client.connect(options);
        client.subscribe(topic, 0);

        CustomMqttCallback customMqttCallback = new CustomMqttCallback(client, topic, mqttPayloadMap);
        client.setCallback(customMqttCallback);
        client.setTimeToWait(10000);

        if (client.isConnected()) {
            log.info("Connected?");

        } else {
            log.info("Not Connected");
        }

        return client;
    }

    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile, final String password) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // load CA certificate
        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caCrtFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client certificate
        bis = new BufferedInputStream(new FileInputStream(crtFile));
        X509Certificate cert = null;
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis);
//             System.out.println(caCert.toString());
        }

        // load client private key
        PEMParser pemParser = new PEMParser(new FileReader(keyFile));
        Object object = pemParser.readObject();
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                .build(password.toCharArray());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                .setProvider("BC");
        KeyPair key;
        if (object instanceof PEMEncryptedKeyPair) {
            System.out.println("Encrypted key - we will use provided password");
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv));
        } else {
            System.out.println("Unencrypted key - no password needed");
            key = converter.getKeyPair((PEMKeyPair) object);
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[] { cert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    @Bean
    public MqttMessage mqttMessage() {
        return new MqttMessage();
    }
}
