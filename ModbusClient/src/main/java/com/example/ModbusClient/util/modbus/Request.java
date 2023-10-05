package com.example.ModbusClient.util.modbus;

import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.dto.modbus.ModbusRequestProperties;
import com.example.ModbusClient.dto.modbus.ReadRequestParameters;
import com.example.ModbusClient.dto.modbus.WriteRequestParameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.ModbusClient.util.parser.ParseAndResponse.byteArrayToHexString;

@Slf4j
@Component
public class Request {

    public void sendFirstRequest(Map<Channel, Integer> serversOrderMap, ModbusRequestProperties modbusRequestProperties, ModbusProtocol modbusProtocol) {
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

    public void secondSchedule(Map<Channel, Integer> serversOrderMap, ModbusProtocol modbusProtocol, Map<String, Object> resultMap, ModbusTCP6266 tcp6266) {

        for (Channel ctx : serversOrderMap.keySet()) {
            try {
//                log.info("resultMap:{} ", resultMap.get("dataModel"));
                if (resultMap != null) {
                    log.info("resultMap:{} ", resultMap.get("dataModel"));
                    Map<String, Object> payloadMap = new ConcurrentHashMap<>(resultMap);
                    log.info("join: {}", payloadMap.get("dataModel"));
                    DataModel joinMap = (DataModel) payloadMap.get("dataModel");
                    sendMqttRequest(ctx, tcp6266, joinMap, modbusProtocol);
                    log.info("joinMap: {}", joinMap.getData().toString());
                    log.info("data : {}", joinMap);
                } else {
                    log.info("Not available Data");
                }

            } catch (Exception e) {
                log.error("Not available Data");
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
                    log.info("send to Modbus RTU Request: {}", byteArrayToHexString(writeRequest));
                } else {
                    log.error("Failed to send request: ", future.cause());
                }
            }
        });
    }

    public void sendMqttRequest(Channel ctx, ModbusTCP6266 tcp6266, DataModel dataModel, ModbusProtocol modbusProtocol) {
        // 이부분은 추후에 값을 주입해서 해줘야함.

        tcp6266.connect();
        if (dataModel != null) {
            int hzSv = Integer.parseInt(dataModel.getData().getHzSv());
            int fanOn = dataModel.getData().getFanOn();
            String host = dataModel.getDeviceInfo().getHost();
            int port = dataModel.getDeviceInfo().getPort();

            List<Integer> values = new ArrayList<>();
            // 맞는지 정확히 할 필요 있음
            // 예상으로는 제어 중 운전관련 on/off 결과와 제어는 6266으로 할 가능성이 있어보인다. remote / local 상태도 마찬가지
            values.add(hzSv);

            log.info("fanOn: {}", fanOn);
            log.info("hzSv: {}", hzSv);
            tcp6266.writeCoilFanOn(fanOn);

            log.info("여기까지는 진행함.");

            for (int v : values) {

                log.info("value: {}", v);
            }

            double rpm = (hzSv * 60) / 0.0166667;

            WriteRequestParameters build = WriteRequestParameters.builder()
                    .startAddress(0x0380)
                    .value(rpm)
                    .build();

            ByteBuf request = Unpooled.buffer();
            byte[] modbusRequest = modbusProtocol.getWriteRequest.apply(build);

            writeMessaging(ctx, request, modbusRequest);
            log.info("성공적으로 제어 신호 보냄");
        }
    }

}
