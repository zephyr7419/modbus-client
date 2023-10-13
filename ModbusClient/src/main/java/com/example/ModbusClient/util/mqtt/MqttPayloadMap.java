package com.example.ModbusClient.util.mqtt;

import com.example.ModbusClient.dto.DataModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MqttPayloadMap {

    private Map<String, Object> resultMap = new ConcurrentHashMap<>();
    private CompletableFuture<DataModel> mqttMessageFuture = new CompletableFuture<>();

    public void saveMap(DataModel dataModel) {
        resultMap.put("dataModel", dataModel);
        log.info("resultMap: {}", resultMap.get("dataModel"));
    }

    public Map<String, Object> getResultMap() {
//        log.info("resultMap2: {}", resultMap.get("dataModel"));

        Map<String, Object> newMap = new ConcurrentHashMap<>(resultMap);
        resultMap.clear();
//        log.info("newMap: {}", newMap.get("dataModel"));
        return newMap;
    }

    public void saveData(DataModel dataModel) {
        resultMap.put("dataModel", dataModel);
    }

    public CompletableFuture<DataModel> waitForMqttMessageAsync() {
//        CompletableFuture<DataModel> currentFuture = mqttMessageFuture;
        CompletableFuture<DataModel> newFuture =  new CompletableFuture<>();
        DataModel latestData = (DataModel) resultMap.get("dataModel");

        if (latestData != null) {
            newFuture.complete(latestData);
        } else {
            mqttMessageFuture.thenRunAsync(() -> newFuture.complete(mqttMessageFuture.join()));
        }

        mqttMessageFuture = new CompletableFuture<>();
        return newFuture;
    }


}
