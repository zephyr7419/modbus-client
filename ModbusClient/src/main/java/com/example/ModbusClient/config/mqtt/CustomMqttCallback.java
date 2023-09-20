package com.example.ModbusClient.config.mqtt;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@AllArgsConstructor
public class CustomMqttCallback implements MqttCallback {

//    private UpEventService upEventService;
    private MqttClient client;
    private String topic;

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        log.error("Mqtt Broker is disconnected: {}", disconnectResponse.getException().getMessage());
        try {
            client.reconnect();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // MQTT 메시지 수신 시의 처리
        byte[] payload = message.getPayload();
        // Base64 디코딩
        byte[] decodedData = Base64.getDecoder().decode(payload);
        String decodedMessage = new String(decodedData, StandardCharsets.UTF_8);
        // 디코딩된 데이터를 사용하여 원하는 작업 수행
        log.info("Received message: {}", decodedMessage);
//        upEventService.parsing(message);
    }

    @Override
    public void deliveryComplete(IMqttToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("Mqtt Broker is Connected");
        try {
            client.subscribe(topic, 0);
            log.info("client: {}", client.subscribe(topic, 0).getMessage().getPayload().length);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {

    }
}
