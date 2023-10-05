package com.example.ModbusClient.config.mqtt;


import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.util.mqtt.MqttMessageParser;
import com.example.ModbusClient.util.mqtt.MqttPayloadMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

@Slf4j
@AllArgsConstructor
public class CustomMqttCallback implements MqttCallback {

    private MqttClient client;
    private String topic;
    private final MqttPayloadMap mqttPayloadMap;

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

        log.info("message arrived");
        MqttMessageParser messageParser = new MqttMessageParser();
        DataModel parse = messageParser.parse(message);

        if (message.getPayload() != null) {
            mqttPayloadMap.saveMap(parse);
        }

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
