package com.example.ModbusClient.service;

import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.example.ModbusClient.util.modbus.ReadRequestParameters;
import com.example.ModbusClient.util.modbus.WriteRequestParameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModbusService {

    private final Map<ChannelHandlerContext, Integer> serversOrderMap = new ConcurrentHashMap<>();
    private final ModbusProtocol modbusProtocol;
    private final ModbusTCP6266 tcp6266;
    private int orderCount = 0;

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
    @Scheduled(fixedRate = 30000)
    public void connectAndRequest() {
        tcp6266.connect();
        for (ChannelHandlerContext ctx : serversOrderMap.keySet()) {
            ByteBuf request = Unpooled.buffer();
            List<Integer> values = new ArrayList<>();
            values.add(0x0080);
            values.add(0x0000);
            values.add(0x0000);
            values.add(0x0000);
            WriteRequestParameters writeRequestParam = WriteRequestParameters.builder()
                    .parameterCount(4)
                    .startAddress(0x0380)
                    .values(values)
                    .build();
            byte[] writeRequest = modbusProtocol.getWriteRequest.apply(writeRequestParam);
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

        int[] addressArray = {0x0304, 0x0006};
        int[] countArray = {0x0002, 0x0007};

        for (int i = 0; i < addressArray.length; i++) {
            for (ChannelHandlerContext ctx : serversOrderMap.keySet()) {
                ByteBuf request = Unpooled.buffer();

                ReadRequestParameters requestParam = ReadRequestParameters.builder()
                        .startAddress(addressArray[i])
                        .quantity(countArray[i])
                        .build();

                byte[] modbusRequest = modbusProtocol.getReadRequest.apply(requestParam);
                request.writeBytes(modbusRequest);

                ChannelFuture future = ctx.writeAndFlush(request);

                if (future.isSuccess()) {
                    log.info("send to Modbus RTU Request: {}", byteArrayToHexString(modbusRequest));
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
