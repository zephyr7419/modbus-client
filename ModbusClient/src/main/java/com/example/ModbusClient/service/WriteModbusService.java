package com.example.ModbusClient.service;

import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.example.ModbusClient.util.modbus.Request;
import com.example.ModbusClient.util.mqtt.MqttPayloadMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class WriteModbusService {
    private final MqttPayloadMap mqttPayloadMap;
    private final Request request;
    private ChannelHandlerContext channelHandlerContext;
    private final ModbusTCP6266 modbusTCP6266;
    private final ModbusProtocol protocol;


    public void writeMessageFromMqtt() {
        CompletableFuture<DataModel> dataModelCompletableFuture = mqttPayloadMap.waitForMqttMessageAsync();
        DataModel join = dataModelCompletableFuture.join();

//        request.sendMqttRequest(getChannelHandlerContext(), modbusTCP6266, join, protocol);

    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannel(ChannelHandlerContext ctx) {
        this.channelHandlerContext = ctx;
    }

}
