package com.example.ModbusClient.service.modbus;

import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.util.mqtt.MqttPayloadMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttConvertTCP {
    private final MqttPayloadMap mqttPayloadMap;

    public CompletableFuture<DataModel> getData() {
        return mqttPayloadMap.waitForMqttMessageAsync();
    }

    public Map<String, Object> getResultMap() {
        Map<String, Object> resultMap = mqttPayloadMap.getResultMap();
        log.info("resultMap: {}", resultMap.get("dataModel"));
        return resultMap;
    }
}
