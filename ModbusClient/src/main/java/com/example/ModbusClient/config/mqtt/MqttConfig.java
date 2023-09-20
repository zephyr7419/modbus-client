package com.example.ModbusClient.config.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MqttConfig {

    @Value("${mqtt.server-url}")
    private String serverUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Bean
    public MqttClient mqttClient() throws Exception {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        options.setCleanStart(false);
        options.setAutomaticReconnect(true);

        MqttClient client = new MqttClient(serverUrl, clientId);

        client.connect(options);
        client.subscribe("application/#", 0);

        CustomMqttCallback customMqttCallback = new CustomMqttCallback(client, topic);
        client.setCallback(customMqttCallback);
        if (client.isConnected()) {
            log.info("Connected?");
        }

        return client;
    }

    @Bean
    public MqttMessage mqttMessage() {
        return new MqttMessage();
    }
}
