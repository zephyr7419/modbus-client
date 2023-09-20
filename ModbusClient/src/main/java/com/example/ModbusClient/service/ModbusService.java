package com.example.ModbusClient.service;

import com.example.ModbusClient.entity.modbus.ModbusRequestProperties;
import com.example.ModbusClient.entity.modbus.ReadRequestParameters;
import com.example.ModbusClient.entity.modbus.WriteRequestParameters;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.ghgande.j2mod.modbus.util.BitVector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModbusService {

    private final Map<ChannelHandlerContext, Integer> serversOrderMap = new ConcurrentHashMap<>();
    private final ModbusProtocol modbusProtocol;
    private final ModbusTCP6266 tcp6266;
    private final BitVector previousCoilValues = new BitVector(4);
    private final ModbusRequestProperties modbusRequestProperties;
    private int orderCount = 0;
    private long startTime;

    /**
     * 서버 연결이 추가 될 때 연결 한 server 를 추가
     * @param ctx
     */
    public void addServer(ChannelHandlerContext ctx) {
        serversOrderMap.put(ctx, ++orderCount);
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
        for (Map.Entry<ChannelHandlerContext, Integer> entry : serversOrderMap.entrySet()) {
            ChannelHandlerContext ctx = entry.getKey();
            int port = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
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
        connectAndCheckCoilValues();
        requestModbus();

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

    private void requestModbus() {


        for (ChannelHandlerContext ctx : serversOrderMap.keySet()) {

            for (ReadRequestParameters readRequestParameters : modbusRequestProperties.getReadRequests()) {
                ByteBuf request = Unpooled.buffer();
                byte[] modbusRequest = modbusProtocol.getReadRequest.apply(readRequestParameters);
                writeMessaging(ctx, request, modbusRequest);
            }
        }

        // mqtt 로 제어 통신이 오면 실행하는 제어
        for (ChannelHandlerContext ctx : serversOrderMap.keySet()) {

            for (WriteRequestParameters writeRequestParameters : modbusRequestProperties.getWriteRequests()) {
                ByteBuf request = Unpooled.buffer();
                byte[] writeRequest = modbusProtocol.getWriteRequest.apply(writeRequestParameters);
                writeMessaging(ctx, request, writeRequest);
            }

        }
    }

    /**
     *
     * @param ctx
     * @param request
     * @param writeRequest
     */
    private void writeMessaging(ChannelHandlerContext ctx, ByteBuf request, byte[] writeRequest) {
        request.writeBytes(writeRequest);

        ChannelFuture future = ctx.writeAndFlush(request);
        if (future.isSuccess()) {
            log.info("send to Modbus RTU Request: {}", byteArrayToHexString(writeRequest));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
