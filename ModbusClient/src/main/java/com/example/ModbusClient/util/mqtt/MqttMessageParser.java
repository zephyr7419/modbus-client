package com.example.ModbusClient.util.mqtt;


import com.example.ModbusClient.dto.DataModel;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttMessage;

@Slf4j
public class MqttMessageParser {

    private DataModel dataModel;

    public DataModel parse(MqttMessage message) {
        log.info("메세지옴");
        byte[] payload = message.getPayload();
        String decodedString = new String(payload);
        Gson gson = new Gson();

        dataModel = gson.fromJson(decodedString, DataModel.class);

//        log.info("decoded message: {}", dataModel.toString());
        return dataModel;
    }

}
