package com.example.ModbusClient.service;

import com.example.ModbusClient.config.netty.ParseAndResponse;
import com.example.ModbusClient.entity.modbus.ModbusRequestProperties;
import com.example.ModbusClient.entity.modbus.ReadRequestParameters;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class ModbusService {

    private final Map<Channel, Integer> serversOrderMap = new ConcurrentHashMap<>();
    private final AtomicReference<byte[]> firstResponse = new AtomicReference<>();
    private final AtomicReference<byte[]> secondResponse = new AtomicReference<>();
    private final ModbusProtocol modbusProtocol;
    private final ModbusTCP6266 tcp6266;
    private final BitVector previousCoilValues = new BitVector(4);
    private final ModbusRequestProperties modbusRequestProperties;
    private int orderCount = 0;
    private long startTime;
    private boolean isFirstRequest = true;

    /**
     * 서버 연결이 추가 될 때 연결 한 server 를 추가
     * @param
     */
    public void addServer(Channel channel) {

        serversOrderMap.put(channel, ++orderCount);
        listClients();
    }

    public void removeServer(ChannelHandlerContext ctx) {
        serversOrderMap.remove(ctx);
    }

    // 연결된 server 목록 출력
    public void listClients() {
        if (serversOrderMap.isEmpty()) {
            log.info("현재 연결된 server가 없습니다.");
            return;
        }

        log.info("연결된 server 목록: \n");
        for (Map.Entry<Channel, Integer> entry : serversOrderMap.entrySet()) {
            Channel ctx = entry.getKey();
            int port = ((InetSocketAddress) ctx.remoteAddress()).getPort();
            int order = entry.getValue();
            log.info("Port: " + port + ", Order: " + order + "\n");
        }
    }


    /**
     * 연결된 서버에 Modbus protocol 을 이용해 스케쥴을 통해 해당 시간마다 요청을 보낸다.
     */
    public void connectAndRequest() {
        startTime = System.currentTimeMillis();
        connectAndCheckCoilValues();

        isFirstRequest = true;
        sendFirstRequest();

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        log.info("작업 수행 시간 (밀리초): {}", elapsedTime);
    }

    private void connectAndCheckCoilValues() {
        tcp6266.connect();
        BitVector currentCoilValues = tcp6266.readCoilValues();

        if (currentCoilValues != null) {
            for (int i = 0; i < currentCoilValues.size(); i++) {
                boolean currentValue = currentCoilValues.getBit(i);
                boolean previousValue = previousCoilValues.getBit(i);
                log.info("current: {}", currentValue);
                if (currentValue != previousValue) {
                    log.info("Coil {} value changed: {}", i, currentValue);

                    previousCoilValues.setBit(i, currentValue);
                }
            }
        }
    }

    public void sendFirstRequest() {

        for (Channel ctx : serversOrderMap.keySet()) {
            try {
                List<ReadRequestParameters> readRequests = modbusRequestProperties.getReadRequests();
                for (ReadRequestParameters readRequestParameters : readRequests) {
                    Thread.sleep(2000);
                    ByteBuf request = Unpooled.buffer();
                    byte[] modbusRequest = modbusProtocol.getReadRequest.apply(readRequestParameters);
                    writeMessaging(ctx, request, modbusRequest);
                }

            } catch (Exception e) {
                log.error("Error occurred while sending second request: ", e);
            }
        }

    }

    /**
     *
     * @param
     * @param request
     * @param writeRequest
     */
    private void writeMessaging(Channel channel, ByteBuf request, byte[] writeRequest) {
        request.writeBytes(writeRequest);

        ChannelFuture future = channel.writeAndFlush(request);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(@NotNull ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("send to Modbus RTU Request: {}", byteArrayToHexString(writeRequest));
                } else {
                    log.error("Failed to send request: ", future.cause());
                }
            }
        });

    }

    public boolean isFirstRequestExecute(boolean flag) {
        isFirstRequest = flag;
        return isFirstRequest;
    }

    public void onFirstResponseReceived(byte[] response) {
        firstResponse.set(response);
        isFirstRequestExecute(false);
        log.info("first response complete!!");
    }

    public void onSecondResponseReceived(byte[] response) {
        secondResponse.set(response);
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(firstResponse.getAndSet(null));

        String status = (String) stringObjectMap.get("Operating Status");
        String[] splitStatus = status.split(",");

        Map<String, Object> parsed = parseAndResponse.parseAndLogResponse(secondResponse.getAndSet(null));
        Object targetFrequency = parsed.get("Target Frequency");
        stringObjectMap.put("Target Frequency", targetFrequency);
        JsonObject jsonObject = parseAndResponse.mqttMessageToParsing(stringObjectMap, splitStatus);
        log.info("first response complete!!");
        log.info("JsonObject: {}", jsonObject);

        connectAndRequest();
    }

    public boolean isFirstResponseExpected() {
        return isFirstRequest;
    }

}
