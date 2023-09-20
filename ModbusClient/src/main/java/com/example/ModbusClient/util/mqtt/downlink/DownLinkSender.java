//package com.example.ModbusClient.util.mqtt.downlink;
//
//import com.example.ModbusClient.util.parser.ByteParser;
//import com.example.ModbusClient.util.parser.NumToByteParser;
//import com.google.gson.JsonObject;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.eclipse.paho.mqttv5.client.MqttClient;
//import org.eclipse.paho.mqttv5.common.MqttException;
//import org.eclipse.paho.mqttv5.common.MqttMessage;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Lazy;
//
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.List;
//
////@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DownLinkSender implements ByteParser, NumToByteParser {
//
//    @Value("${spring.mqtt.application-id}")
//    private String applicationId;
//
//    @Value("${spring.mqtt.fPort}")
//    private int fPort;
//
//    @Lazy
//    private final MqttClient client;
//
//    public void sendDownLink(DownFormat downFormat, String devEui) throws MqttException {
//        List<Byte> downPayload = convertDownFormatToByteList(downFormat);
//        byte[] downEvent = convertByteListToByteArray(downPayload);
//
//        MqttMessage message = createMqttMessage(downEvent);
//        client.publish("application/" + applicationId + "/device/" + devEui + "/command/down", message);
//    }
//
//    private MqttMessage createMqttMessage(byte[] downEvent) {
//        MqttMessage message = new MqttMessage();
//        JsonObject obj = new JsonObject();
//
//        try {
//            obj.addProperty("confirmed", true);
//            obj.addProperty("fPort", fPort);
//            obj.addProperty("data", Base64.getEncoder().encodeToString(downEvent));
//        } catch (Exception e) {
//            log.error("Json Error: {}", e.getMessage());
//        }
//        message.setQos(0);
//        message.setPayload(obj.toString().getBytes());
//        return message;
//    }
//
//    private byte[] convertByteListToByteArray(List<Byte> downPayload) {
//        byte[] downEvent = new byte[downPayload.size()];
//        for (int i = 0; i < downPayload.size(); i++) {
//            downEvent[i] = downPayload.get(i);
//        }
//        return downEvent;
//    }
//
//    private List<Byte> convertDownFormatToByteList(DownFormat downFormat) {
//        List<Byte> downPayload = new ArrayList<>();
//
//        downPayload.addAll(convertAscII(downFormat.getHouseFanStatus()));
//        downPayload.addAll(convertOneByte(downFormat.getFanMotorRPM()));
//        downPayload.addAll(convertAscII(String.valueOf(downFormat.getMode())));
//        downPayload.addAll(convertAscII(downFormat.getAirflowRate()));
//        downPayload.addAll(convertAscII(downFormat.getVoltage()));
//        downPayload.addAll(convertAscII(downFormat.getCurrent()));
//        downPayload.addAll(convertAscII(downFormat.getPowerConsumption()));
//
//        return downPayload;
//    }
//
//}
