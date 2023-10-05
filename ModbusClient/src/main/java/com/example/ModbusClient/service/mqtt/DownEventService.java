package com.example.ModbusClient.service.mqtt;

import com.example.ModbusClient.dto.modbus.ServerInfo;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownEventService {
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

        message.setQos(0);
        message.setPayload(mqttMessage.toString().getBytes());

        try {
            mqttClient.publish("application/test", message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }

    private String convertMapToJsonAndEncodeBase64(JsonObject jsonObject) {
        String jsonData = String.valueOf(jsonObject);
        log.info("jsonData: {}", jsonData);

        // JSON 문자열을 바이트 배열로 변환한 후 Base64로 인코딩
        byte[] jsonDataBytes = jsonData.getBytes();
        byte[] base64Data = Base64.getEncoder().encode(jsonDataBytes);

        return new String(base64Data);
    }

}
