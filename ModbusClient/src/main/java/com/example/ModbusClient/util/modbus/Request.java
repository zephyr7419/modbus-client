package com.example.ModbusClient.util.modbus;

import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.entity.modbus.ModbusRequestProperties;
import com.example.ModbusClient.entity.modbus.ReadRequestParameters;
import com.example.ModbusClient.entity.modbus.WriteRequestParameters;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Slf4j
@Component
public class Request {

    public void sendFirstRequest(Map<Channel, Integer> serversOrderMap, ModbusRequestProperties modbusRequestProperties, ModbusProtocol modbusProtocol) throws InterruptedException {
        Thread.sleep(10000);
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

    public void sendSecondRequest(Map<Channel, Integer> serversOrderMap, ModbusRequestProperties modbusRequestProperties, ModbusProtocol modbusProtocol) {
        for (Channel ctx : serversOrderMap.keySet()) {
            try {
                List<ReadRequestParameters> readRequests = modbusRequestProperties.getReadRequests();
                ReadRequestParameters readRequestParameters = readRequests.get(2);

                ByteBuf request = Unpooled.buffer();
                byte[] modbusRequest = modbusProtocol.getReadRequest.apply(readRequestParameters);
                writeMessaging(ctx, request, modbusRequest);

            } catch (Exception e) {
                log.error("Error occurred while sending second request: ", e);
            }
        }
    }

    public void sendThirdRequest(Map<Channel, Integer> serversOrderMap, ModbusRequestProperties modbusRequestProperties, ModbusProtocol modbusProtocol) {
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

    private void writeMessaging(Channel channel, ByteBuf request, byte[] writeRequest) {
        request.writeBytes(writeRequest);

        ChannelFuture future = channel.writeAndFlush(request);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(@NotNull ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Thread.sleep(5000);
                    log.info("send to Modbus RTU Request: {}", byteArrayToHexString(writeRequest));
                } else {
                    log.error("Failed to send request: ", future.cause());
                }
            }
        });
    }

    public void sendMqttRequest(ChannelHandlerContext ctx, ModbusTCP6266 tcp6266, DataModel dataModel, ModbusProtocol modbusProtocol) {
        // 이부분은 추후에 값을 주입해서 해줘야함.

        tcp6266.connect();
        int hzSv = Integer.parseInt(dataModel.getData().getHzSv());
        int fanOn = dataModel.getData().getFanOn();
        String host = dataModel.getDeviceInfo().getHost();
        int port = dataModel.getDeviceInfo().getPort();

        List<Integer> values = new ArrayList<>();
        // 맞는지 정확히 할 필요 있음
        // 예상으로는 제어 중 운전관련 on/off 결과와 제어는 6266으로 할 가능성이 있어보인다. remote / local 상태도 마찬가지
        values.add(hzSv);
        values.add(0x0000);

        log.info("fanOn: {}", fanOn);
        log.info("hzSv: {}", hzSv);
        tcp6266.writeCoilFanOn(fanOn);

        log.info("여기까지는 진행함.");

        for (int v : values) {

            log.info("value: {}", v);
        }

        WriteRequestParameters build = WriteRequestParameters.builder()
                .startAddress(0x0380)
                .value(hzSv)
                .build();

        ByteBuf request = Unpooled.buffer();
        byte[] modbusRequest = modbusProtocol.getWriteRequest.apply(build);

        writeMessaging(ctx.channel(), request, modbusRequest);
        log.info("성공적으로 제어 신호 보냄");
    }

}
