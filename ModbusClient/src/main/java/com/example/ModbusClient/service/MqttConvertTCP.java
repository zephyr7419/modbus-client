package com.example.ModbusClient.service;

import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.entity.modbus.ServerInfo;
import com.example.ModbusClient.util.modbus.Request;
import com.example.ModbusClient.util.mqtt.MqttPayloadMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttConvertTCP {
    private final MqttPayloadMap mqttPayloadMap;
    private final Request request;

//    public void checkingMqttMessage() {
//        if (mqttPayloadMap.waitForMqttMessageAsync().isDone()) {
//
//        } else {
//            log.info("mqtt message not arrived");
//
//        }
//    }

    public CompletableFuture<DataModel> getData() {
        return mqttPayloadMap.waitForMqttMessageAsync();
    }

    public void connectedServer(ChannelHandlerContext ctx) {
        CompletableFuture<DataModel> dataModelCompletableFuture = mqttPayloadMap.waitForMqttMessageAsync();
        DataModel join = dataModelCompletableFuture.join();


        ServerInfo deviceInfo = join.getDeviceInfo();

        String host = deviceInfo.getHost();
        int port = deviceInfo.getPort();

//        request.sendMqttRequest(ctx, new ModbusTCP6266(), join, new ModbusProtocol());

    }

//    private void convertTcp() {
//
//        if (connect.isSuccess() && channel.isActive()) {
//
//            WriteRequestParameters build = WriteRequestParameters.builder()
//                    .startAddress(0x0380)
//                    .value(Integer.parseInt(data.getHzSv()))
//                    .build();
//
//            ByteBuf byteBuf = Unpooled.buffer();
//            byte[] request = modbusProtocol.getWriteRequest.apply(build);
//            writeMessaging(channel, byteBuf, request);
//
//        }
//
//        JsonObject jsonObject = new JsonObject();
//        jsonObject.addProperty("HZ_SV", data.getHzSv());
//        jsonObject.addProperty("FAN_ON", data.getFanOn());
//        jsonObject.addProperty("REMOTE", data.isRemote());
//
//        log.info("fanOn: {}", data.getFanOn());
//        log.info("hzSv: {}", data.getHzSv());
//
//    }

}
