package com.example.ModbusClient.util.mqtt;

import com.example.ModbusClient.dto.DataModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MqttPayloadMap {

    private final Map<String, Object> resultMap = new ConcurrentHashMap<>();
    private final CompletableFuture<DataModel> mqttMessageFuture = new CompletableFuture<>();

    public boolean saveData(DataModel dataModel) {
        log.info("here message");
        return mqttMessageFuture.complete(dataModel);
    }

    public CompletableFuture<DataModel> waitForMqttMessageAsync() {
        return mqttMessageFuture;
    }

    public void savePayload(DataModel dataModel) {
        resultMap.put("dateModel", dataModel);
    }

    public Map<String, Object> getResultMap() {
        return resultMap;
    }

    public void clearResultMap() {
        resultMap.clear();
    }
}
