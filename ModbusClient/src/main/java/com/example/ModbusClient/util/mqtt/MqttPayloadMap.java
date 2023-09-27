package com.example.ModbusClient.util.mqtt;

import com.example.ModbusClient.dto.DataModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class MqttPayloadMap {

    private Map<String, Object> resultMap = new ConcurrentHashMap<>();

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
