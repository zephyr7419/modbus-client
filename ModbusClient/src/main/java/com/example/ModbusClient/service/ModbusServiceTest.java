package com.example.ModbusClient.service;

import com.example.ModbusClient.config.netty.ParseAndResponse;
import com.example.ModbusClient.dto.Data;
import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.entity.modbus.ModbusRequestProperties;
import com.example.ModbusClient.entity.modbus.ReadRequestParameters;
import com.example.ModbusClient.entity.modbus.ServerInfo;
import com.example.ModbusClient.entity.modbus.WriteRequestParameters;
import com.example.ModbusClient.util.InfluxManager;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.example.ModbusClient.util.modbus.Request;
import com.example.ModbusClient.util.mqtt.MqttPayloadMap;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class ModbusServiceTest {

    private final Map<Channel, Integer> serversOrderMap = new ConcurrentHashMap<>();
    private final AtomicReference<byte[]> firstResponse = new AtomicReference<>();
    private final AtomicReference<byte[]> secondResponse = new AtomicReference<>();
    private final AtomicReference<byte[]> thirdResponse = new AtomicReference<>();
    private final ModbusProtocol modbusProtocol;
    private final ModbusTCP6266 tcp6266;
    private final BitVector previousDiCoilValues = new BitVector(4);
    private final BitVector previousDoCoilValues = new BitVector(4);
    private final ModbusRequestProperties modbusRequestProperties;
    private int orderCount = 0;
    private long startTime;
    private final InfluxManager influxManager;
    private final Request request;
    private final MqttConvertTCP mqttConvertTCP;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startScheduling() {
        scheduler.scheduleWithFixedDelay(this::firstSchedule, 0, 60, TimeUnit.SECONDS);
    }

    public void firstSchedule() {

        sendFirstRequest();
        scheduler.schedule(this::secondRequest, 2, TimeUnit.SECONDS);
        scheduler.schedule(this::thirdRequest, 2, TimeUnit.SECONDS);
        scheduler.schedule(this::sendFirstRequest, 10, TimeUnit.SECONDS);
        scheduler.schedule(this::secondSchedule, 5, TimeUnit.SECONDS);

    }

    public void secondRequest() {
        for (Map.Entry<Channel, Integer> entry : serversOrderMap.entrySet()) {
            Channel key = entry.getKey();
            request.sendSecondRequest(key, modbusRequestProperties, modbusProtocol);
        }
    }

    public void thirdRequest() {
        for (Map.Entry<Channel, Integer> entry : serversOrderMap.entrySet()) {
            Channel key = entry.getKey();
            request.sendThirdRequest(key, modbusRequestProperties, modbusProtocol);
        }
    }

    public void secondSchedule() {

        CompletableFuture<DataModel> data = mqttConvertTCP.getData();
        DataModel join = data.join();
        for (Map.Entry<Channel, Integer> entry : serversOrderMap.entrySet()) {
            Channel channel = entry.getKey();

            request.sendMqttRequest(channel, tcp6266, join, modbusProtocol);

        }
        log.info("data : {}", join);

    }
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

    public Map<Channel, Integer> getConnectingServer() {
        return serversOrderMap;
    }

    public void sendFirstRequest()  {
        try {
            request.sendFirstRequest(serversOrderMap, modbusRequestProperties, modbusProtocol);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void heartbeatRequest() throws InterruptedException {
        startTime = System.currentTimeMillis();
        sendFirstRequest();

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        log.info("작업 수행 시간 (밀리초): {}", elapsedTime);
    }

    public BitVector getTcp6266Bit() {
        return tcp6266.readDoCoilValues();
    }

    private List<BitVector> connectAndCheckCoilValues() {
        tcp6266.connect();
        BitVector currentDiCoilValues = tcp6266.readDiCoilValues();
        BitVector currentDoCoilValues = tcp6266.readDoCoilValues();

        BitVector diVector = changeCheck(currentDiCoilValues, previousDiCoilValues);

        BitVector doVector = changeCheck(currentDoCoilValues, previousDoCoilValues);
        List<BitVector> bitVectors = new ArrayList<>();
        bitVectors.add(diVector);
        bitVectors.add(doVector);
        return bitVectors;
    }

    private BitVector changeCheck(BitVector currentDoCoilValues, BitVector previousDoCoilValues) {
        if (currentDoCoilValues != null) {
            for (int i = 0; i < currentDoCoilValues.size(); i++) {
                boolean currentValue = currentDoCoilValues.getBit(i);
                boolean previousValue = previousDoCoilValues.getBit(i);
                log.info("current: {}", currentValue);
                if (currentValue != previousValue) {
                    log.info("Coil {} value changed: {}", i, currentValue);

                    previousDoCoilValues.setBit(i, currentValue);
                }
            }
        }

        return currentDoCoilValues;
    }

    public void onFirstResponseReceived(byte[] response, ChannelHandlerContext ctx) throws InterruptedException {

        firstResponse.set(response);
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(firstResponse.getAndSet(null));

        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        ServerInfo device1 = ServerInfo.builder()
                .name("device1")
                .host(socketAddress.getAddress().getHostAddress())
                .port(socketAddress.getPort())
                .build();


        String status = (String) stringObjectMap.get("Operation Status");
        String[] splitStatus = status.split(",");

        JsonObject jsonObject = parseAndResponse.mqttMessageToParsing(stringObjectMap, splitStatus);

        Map<String, Object> saveData = new HashMap<>(stringObjectMap);
        saveData.put("deviceIP", device1.getHost());
        saveData.put("devicePort", device1.getPort());
        influxManager.saveDataToInfluxDB(saveData);

//        handleScheduleUpdate(device1, stringObjectMap);

        sendMqttMessage(device1, jsonObject);
        log.info("first response complete!!");
        Thread.sleep(5000);

    }

    public void onSecondResponseReceived(byte[] response, ChannelHandlerContext ctx) throws InterruptedException {
        secondResponse.set(response);
        Thread.sleep(5000);

        log.info("first response complete!!");

    }

    public void writeResponseParsing(byte[] data, ChannelHandlerContext ctx) throws InterruptedException {

        tcp6266.connect();
        BitVector tcp6266Bit = getTcp6266Bit();
        boolean fanOn = tcp6266Bit.getBit(0);
        boolean remote = tcp6266Bit.getBit(1);
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(data);

        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        ServerInfo device1 = ServerInfo.builder()
                .name("device1")
                .host(socketAddress.getAddress().getHostAddress())
                .port(socketAddress.getPort())
                .build();

//        mqttPayloadMap.clearResultMap();

        request.sendFirstRequest(serversOrderMap, modbusRequestProperties, modbusProtocol);

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

            String topic = "application/" + "4" + "/device/command/down";
            MqttMessage message = new MqttMessage(jsonDataBase64.getBytes());
            message.setQos(0);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            log.error("Json Error: {}", e.getMessage());
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

    public void onThirdResponseReceived(byte[] response, ChannelHandlerContext ctx) throws InterruptedException {
        thirdResponse.set(response);

        ParseAndResponse parseAndResponse = new ParseAndResponse();
        List<BitVector> bitVectors = connectAndCheckCoilValues();
        BitVector diVector = bitVectors.get(0);
        BitVector doVector = bitVectors.get(1);


        Map<String, Object> parsed = parseAndResponse.parseAndLogResponse(secondResponse.getAndSet(null));
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(thirdResponse.getAndSet(null));

        String status = (String) stringObjectMap.get("Operating Status");
        String[] splitStatus = status.split(",");

        Map<String, Object> saveData = new HashMap<>(parsed);
        saveData.putAll(stringObjectMap);
        String targetFrequency = (String) parsed.get("Target Frequency");

        JsonObject jsonObject = new JsonObject();
        boolean inFanOn = diVector.getBit(0);
//        boolean inRemote = diVector.getBit(1);
        boolean outFanOn = doVector.getBit(0);
//        boolean outRemote = doVector.getBit(1);
        jsonObject.addProperty("HZ_PV", targetFrequency);
        jsonObject.addProperty("FAN_ON", !outFanOn ? 0 : 1);
        jsonObject.addProperty("REMOTE", splitStatus[0].equals("HAND") ? 0: 1);

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

        Thread.sleep(5000);


    }
}
