package com.example.ModbusClient.service.modbus;

import com.example.ModbusClient.util.parser.ParseAndResponse;
import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.dto.modbus.ModbusRequestProperties;
import com.example.ModbusClient.dto.modbus.ServerInfo;
import com.example.ModbusClient.service.mqtt.DownEventService;
import com.example.ModbusClient.util.InfluxManager;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.example.ModbusClient.util.modbus.Request;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.google.gson.JsonObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class ModbusService {

    private final Map<Channel, Integer> serversOrderMap = new ConcurrentHashMap<>();
    private final AtomicReference<byte[]> firstResponse = new AtomicReference<>();
    private final AtomicReference<byte[]> secondResponse = new AtomicReference<>();
    private final AtomicReference<byte[]> thirdResponse = new AtomicReference<>();
    private final AtomicReference<DataModel> dataModelAtomicReference = new AtomicReference<>();
    private final ModbusProtocol modbusProtocol;
    private final ModbusTCP6266 tcp6266;
    private final BitVector previousDiCoilValues = new BitVector(4);
    private final BitVector previousDoCoilValues = new BitVector(4);
    private final ModbusRequestProperties modbusRequestProperties;
    private int orderCount = 0;
    private final InfluxManager influxManager;
    private final Request request;
    private final MqttConvertTCP mqttConvertTCP;
    private final DownEventService downEventService;
    private Map<String, Object> payloadMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startScheduling() {
        scheduler.scheduleAtFixedRate(this::firstSchedule, 0, 60, TimeUnit.SECONDS);
    }

    @Scheduled(fixedRate = 60000)
    public void firstSchedule() {
        try {

            log.info("첫 스케쥴 시작");
            request.sendFirstRequest(serversOrderMap, modbusRequestProperties, modbusProtocol);

            scheduler.schedule(() -> request.sendSecondRequest(serversOrderMap, modbusRequestProperties, modbusProtocol), 10, TimeUnit.SECONDS);
            scheduler.schedule(() -> request.sendThirdRequest(serversOrderMap, modbusRequestProperties, modbusProtocol), 20, TimeUnit.SECONDS);
            scheduler.schedule(() -> request.sendFirstRequest(serversOrderMap, modbusRequestProperties, modbusProtocol), 30, TimeUnit.SECONDS);
            scheduler.schedule(() -> request.secondSchedule(serversOrderMap, modbusProtocol, payloadMap, tcp6266), 35, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("schedule is error: {}", e.getMessage());
        }

    }

    @Scheduled(fixedRate = 60000)
    private void heartbeatMqttSubscribe() {
        Map<String, Object> resultMap = mqttConvertTCP.getResultMap();
        payloadMap.putAll(resultMap);
        resultMap.clear();
        log.info("payloadMap: {}", payloadMap.get("dataModel"));
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

    public BitVector getTcp6266Bit() {
        return tcp6266.readDiCoilValues();
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
        Thread.sleep(200);
        List<BitVector> bitVectors = connectAndCheckCoilValues();
        BitVector diVector = bitVectors.get(0);
        boolean outFanOn = diVector.getBit(1);
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

        JsonObject jsonObject = parseAndResponse.mqttMessageToParsing(stringObjectMap, splitStatus, outFanOn);

        Map<String, Object> saveData = new HashMap<>(stringObjectMap);
        saveData.put("deviceIP", device1.getHost());
        saveData.put("devicePort", device1.getPort());
        influxManager.saveDataToInfluxDB(saveData);

        downEventService.sendDownLinkEvent(device1, jsonObject);
        log.info("first response complete!!");

    }

    public void onSecondResponseReceived(byte[] response) throws InterruptedException {
        secondResponse.set(response);
        Thread.sleep(200);

        log.info("first response complete!!");
    }

    public void writeResponseParsing(byte[] data, ChannelHandlerContext ctx) throws InterruptedException {

        tcp6266.connect();
        BitVector tcp6266Bit = getTcp6266Bit();
        boolean fanOn = tcp6266Bit.getBit(0);
        boolean remote = tcp6266Bit.getBit(1);
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(data);

        int value = (int) stringObjectMap.get("value");

        log.info("hz_sv: {}", value);
        JsonObject jsonObject = new JsonObject();

        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        ServerInfo device1 = ServerInfo.builder()
                .name("device1")
                .host(socketAddress.getAddress().getHostAddress())
                .port(socketAddress.getPort())
                .build();

        jsonObject.addProperty("HZ_SV", value);
        jsonObject.addProperty("FAN_ON", !fanOn ? 0 : 1);
        downEventService.sendDownLinkEvent(device1, jsonObject);
    }

    public void onThirdResponseReceived(byte[] response, ChannelHandlerContext ctx) throws InterruptedException {
        thirdResponse.set(response);
        Thread.sleep(200);
        ParseAndResponse parseAndResponse = new ParseAndResponse();
        List<BitVector> bitVectors = connectAndCheckCoilValues();
        BitVector diVector = bitVectors.get(0);
        boolean outFanOn = diVector.getBit(1);

        Map<String, Object> parsed = parseAndResponse.parseAndLogResponse(secondResponse.getAndSet(null));
        Map<String, Object> stringObjectMap = parseAndResponse.parseAndLogResponse(thirdResponse.getAndSet(null));

        String status = (String) stringObjectMap.get("Operating Status");
        String[] splitStatus = status.split(",");

        Map<String, Object> saveData = new HashMap<>(parsed);
        saveData.putAll(stringObjectMap);
        String targetFrequency = (String) parsed.get("Target Frequency");

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("HZ_PV", targetFrequency);
        jsonObject.addProperty("FAN_ON", !outFanOn ? 0 : 1);
        jsonObject.addProperty("REMOTE", splitStatus[0].equals("HAND") ? 0 : 1);

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

        downEventService.sendDownLinkEvent(device1, jsonObject);

    }


//    /**
//     * 응답받은 데이터를 파싱한 Map 을 base64 형식으로 변환
//     */
//    private String convertMapToJsonAndEncodeBase64(JsonObject jsonObject) {
//        String jsonData = String.valueOf(jsonObject);
//        log.info("jsonData: {}", jsonData);
//
//        // JSON 문자열을 바이트 배열로 변환한 후 Base64로 인코딩
//        byte[] jsonDataBytes = jsonData.getBytes();
//        byte[] base64Data = Base64.getEncoder().encode(jsonDataBytes);
//
//        return new String(base64Data);
//    }
}
