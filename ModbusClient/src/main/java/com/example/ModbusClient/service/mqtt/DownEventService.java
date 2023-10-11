package com.example.ModbusClient.service.mqtt;

import com.example.ModbusClient.dto.modbus.ServerInfo;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownEventService {
    @Lazy
    private final MqttClient mqttClient;

    public void sendDownLinkEvent(ServerInfo serverInfo, JsonObject data) {
        MqttMessage message = new MqttMessage();
        JsonObject deviceInfo = new JsonObject();
        JsonObject mqttMessage = new JsonObject();
        deviceInfo.addProperty("name", serverInfo.getName());
        deviceInfo.addProperty("host", serverInfo.getHost());
        deviceInfo.addProperty("port", serverInfo.getPort());

        mqttMessage.add("deviceInfo", deviceInfo);
        mqttMessage.add("data", data);

        byte[] s = convertMapToJsonAndEncodeBase64(mqttMessage);

        message.setQos(0);
        message.setPayload(s);

        try {
            boolean connected = mqttClient.isConnected();
            log.info("connected: {}", connected);
            if (connected) {
                mqttClient.publish("application/test", message);
            } else {
                mqttClient.reconnect();
                mqttClient.publish("application/test", message);
            }
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }

    private byte[] convertMapToJsonAndEncodeBase64(JsonObject jsonObject) {
        String jsonData = String.valueOf(jsonObject);
        log.info("jsonData: {}", jsonData);

        // JSON 문자열을 바이트 배열로 변환한 후 Base64로 인코딩
        byte[] jsonDataBytes = jsonData.getBytes();

        return Base64.getEncoder().encode(jsonDataBytes);
    }

}
