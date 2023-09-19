package com.example.ModbusClient.util.mqtt.uplink;

import com.example.ModbusClient.dto.ParseData;
import com.example.ModbusClient.util.parser.BitParser;
import com.example.ModbusClient.util.parser.ByteParser;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.chirpstack.api.as.integration.UplinkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class UplinkParser implements ByteParser, BitParser {

    private final ServiceProperties properties;
    private List<String> serviceTypeList = Arrays.asList("0", "1");

    public ParseData uplinkParsing(byte[] message) throws InvalidProtocolBufferException {
        log.info("Mqtt message: {}", byteToHex(message));

        UplinkEvent uplinkEvent = UplinkEvent.parseFrom(message);
        log.info("Uplink data: {}", byteToHex(uplinkEvent.getData().toByteArray()));

        JsonObject parseData = parseDataPayload(uplinkEvent.getData().toByteArray());

        return ParseData.builder()
                .data(parseData)
                .build();
    }

    private JsonObject parseDataPayload(byte[] byteArray) {
        JsonObject data = new JsonObject();

        return data;
    }
}
