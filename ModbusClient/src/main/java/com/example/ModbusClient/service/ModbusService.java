package com.example.ModbusClient.service;

import com.example.ModbusClient.config.netty.ParseAndResponse;
import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.entity.modbus.ModbusRequestProperties;
import com.example.ModbusClient.entity.modbus.ReadRequestParameters;
import com.example.ModbusClient.entity.modbus.ServerInfo;
import com.example.ModbusClient.entity.modbus.WriteRequestParameters;
import com.example.ModbusClient.util.InfluxManager;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.google.gson.JsonObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class ModbusService {

    private final Map<Channel, Integer> serversOrderMap = new ConcurrentHashMap<>();
    private Map<String, Object> previousData = new ConcurrentHashMap<>();
    private final AtomicReference<byte[]> firstResponse = new AtomicReference<>();
    private final AtomicReference<byte[]> secondResponse = new AtomicReference<>();
    private final ModbusProtocol modbusProtocol;
    private final ModbusTCP6266 tcp6266;
    private final BitVector previousCoilValues = new BitVector(4);
    private final ModbusRequestProperties modbusRequestProperties;
    private int orderCount = 0;
    private long startTime;
    private boolean isFirstRequest = true;
    private final InfluxManager influxManager;

    /**
     * 서버 연결이 추가 될 때 연결 한 server 를 추가
     */
    public void addServer(Channel channel) {

        serversOrderMap.put(channel, ++orderCount);
        listClients();
    }

    public void removeServer(Channel ctx) {
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
    @Scheduled(fixedRate = 60000)
    public void connectAndRequest() {
        startTime = System.currentTimeMillis();

        isFirstRequest = true;
        sendSecondRequest();

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        log.info("작업 수행 시간 (밀리초): {}", elapsedTime);
    }

    @Scheduled(fixedRate = 30000)
    public void heartbeatRequest() {
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
                ReadRequestParameters readRequestParameters = readRequests.get(1);

                ByteBuf request = Unpooled.buffer();
                byte[] modbusRequest = modbusProtocol.getReadRequest.apply(readRequestParameters);
                writeMessaging(ctx, request, modbusRequest);

            } catch (Exception e) {
                log.error("Error occurred while sending second request: ", e);
            }
        }

    }

    public void sendSecondRequest() {

        for (Channel ctx : serversOrderMap.keySet()) {
            try {
                List<ReadRequestParameters> readRequests = modbusRequestProperties.getReadRequests();
                ReadRequestParameters readRequestParameters = readRequests.get(0);

                ByteBuf request = Unpooled.buffer();
                byte[] modbusRequest = modbusProtocol.getReadRequest.apply(readRequestParameters);
                writeMessaging(ctx, request, modbusRequest);

            } catch (Exception e) {
                log.error("Error occurred while sending second request: ", e);
            }
        }

    }

    /**
     *
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

    public void onFirstResponseReceived(byte[] response, ChannelHandlerContext ctx) {
        firstResponse.set(response);
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(firstResponse.getAndSet(null));

        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        ServerInfo device1 = ServerInfo.builder()
                .name("device1")
                .host(socketAddress.getAddress().getHostAddress())
                .port(socketAddress.getPort())
                .build();

        Map<String, Object> saveData = new HashMap<>(stringObjectMap);
        saveData.put("deviceIP", device1.getHost());
        saveData.put("devicePort", device1.getPort());
        influxManager.saveDataToInfluxDB(saveData);

        handleScheduleUpdate(device1, stringObjectMap);

        log.info("first response complete!!");
    }

    private void handleScheduleUpdate(ServerInfo serverInfo, Map<String, Object> newData) {
        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            String key = entry.getKey();
            Object currentValue = entry.getValue();
            Object previousValue = previousData.get(key);

            if (!Objects.equals(currentValue, previousValue)) {
                ParseAndResponse parseAndResponse = new ParseAndResponse();

                String status = (String) newData.get("Operating Status");
                String[] splitStatus = status.split(",");

                JsonObject jsonObject = parseAndResponse.mqttMessageToParsing(newData, splitStatus);
                sendMqttMessage(serverInfo, jsonObject);
                previousData.put(key, currentValue);
            }
        }
    }

    public void onSecondResponseReceived(byte[] response, ChannelHandlerContext ctx) throws InterruptedException {
        secondResponse.set(response);
        Thread.sleep(1000);
        ParseAndResponse parseAndResponse = new ParseAndResponse();

        Map<String, Object> parsed = parseAndResponse.parseAndLogResponse(secondResponse.getAndSet(null));
        Map<String, Object> saveData = new HashMap<>(parsed);
        String targetFrequency = (String) parsed.get("Target Frequency");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("HZ_PV", targetFrequency);

        log.info("target frequency: {}", targetFrequency);
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        ServerInfo device1 = ServerInfo.builder()
                .name("device1")
                .host(socketAddress.getAddress().getHostAddress())
                .port(socketAddress.getPort())
                .build();

        saveData.put("deviceIP", device1.getHost());
        saveData.put("devicePort", device1.getPort());

        influxManager.saveDataToInfluxDB(saveData);

        sendMqttMessage(device1, jsonObject);
        log.info("first response complete!!");
        log.info("JsonObject: {}", jsonObject);
        Thread.sleep(5000);
    }

    public void writeRequest(DataModel dataModel) {
//        tcp6266.writeCoilValues();
        List<WriteRequestParameters> writeRequestParameters = modbusRequestProperties.getWriteRequests();
        // 이부분은 추후에 값을 주입해서 해줘야함.

        int hzSv = Integer.parseInt(dataModel.getData().getHzSv());
        int fanOn = dataModel.getData().getFanOn();
        String host = dataModel.getDeviceInfo().getHost();
        int port = dataModel.getDeviceInfo().getPort();

        List<Integer> values = new ArrayList<>();
        // 맞는지 정확히 할 필요 있음
        // 예상으로는 제어 중 운전관련 on/off 결과와 제어는 6266으로 할 가능성이 있어보인다. remote / local 상태도 마찬가지
        if (fanOn == 0) {
            values.add(0x0000);
        } else {
            fanOn = 0x1000;
            values.add(fanOn);
        }
        values.add(hzSv);

        WriteRequestParameters build = WriteRequestParameters.builder()
                .startAddress(0x0004)
                .values(values)
                .build();

        ByteBuf request = Unpooled.buffer();
        byte[] modbusRequest = modbusProtocol.getWriteRequest.apply(build);

        Bootstrap bootstrap =  new Bootstrap();
        ChannelFuture future = bootstrap.connect(host, port);

        writeMessaging(future.channel(), request, modbusRequest);
        log.info("성공적으로 제어 신호 보냄");
    }


    public void writeResponseParsing(byte[] data) {
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        parseAndResponse.parseAndLogResponse(data);
    }

    private void sendMqttMessage(ServerInfo serverInfo, JsonObject data) {
        JsonObject deviceInfo = new JsonObject();
        JsonObject mqttMessage = new JsonObject();
        deviceInfo.addProperty("name", serverInfo.getName());
        deviceInfo.addProperty("host", serverInfo.getHost());
        deviceInfo.addProperty("port", serverInfo.getPort());

        mqttMessage.add("deviceInfo", deviceInfo);
        mqttMessage.add("data", data);

        String jsonDataBase64 = convertMapToJsonAndEncodeBase64(mqttMessage);

        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttConnectionOptions connectionOptions = new MqttConnectionOptions();
            MqttClient mqttClient = new MqttClient("tcp://localhost:1883", "zephyr", persistence);
            mqttClient.connect(connectionOptions);

            String topic = "application/+/d";
            MqttMessage message = new MqttMessage(jsonDataBase64.getBytes());
            message.setQos(0);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 응답받은 데이터를 파싱한 Map 을 base64 형식으로 변환
     */
    private String convertMapToJsonAndEncodeBase64(JsonObject jsonObject) {
        String jsonData = String.valueOf(jsonObject);
        log.info("jsonData: {}", jsonData);

        // JSON 문자열을 바이트 배열로 변환한 후 Base64로 인코딩
        byte[] jsonDataBytes = jsonData.getBytes();
        byte[] base64Data = Base64.getEncoder().encode(jsonDataBytes);

        return new String(base64Data);
    }

}
