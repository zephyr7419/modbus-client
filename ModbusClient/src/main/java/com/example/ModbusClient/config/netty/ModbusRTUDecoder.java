package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.util.InfluxManager;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;


@RequiredArgsConstructor
@Slf4j
public class ModbusRTUDecoder extends ByteToMessageDecoder {

    private final InfluxManager influxManager;

    /**
     * 응답이 오게 되면 mqtt 통신을 통해 publish 한 후 influx db 에 저장한다.
     * @param ctx           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in            the {@link ByteBuf} from which to read data
     * @param out           the {@link List} to which decoded messages should be added
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= 8) {
            int byteCount = in.getByte(2) & 0xFF;
            if (in.readableBytes() >= byteCount + 5) {
                ByteBuf response = in.readBytes(byteCount + 5);
                log.info("response: {}", response);
                byte[] responseData = new byte[response.readableBytes()];
                response.readBytes(responseData);

                ParseAndResponse parseAndLogResponse = new ParseAndResponse();
                Map<String, Object> stringObjectMap = parseAndLogResponse.parseAndLogResponse(responseData);

                log.info("Received Modbus RTU Response : {}", byteArrayToHexString(responseData));
                String jsonDataBase64 = convertMapToJsonAndEncodeBase64(stringObjectMap);
                MemoryPersistence persistence = new MemoryPersistence();

                MqttClient mqttClient = new MqttClient("tcp://localhost:1883", "zephyr", persistence);
                MqttConnectionOptions connectionOptions = new MqttConnectionOptions();

                mqttClient.connect(connectionOptions);

                String topic = "application/test";
                MqttMessage message = new MqttMessage(jsonDataBase64.getBytes());
                message.setQos(0);
                mqttClient.publish(topic, message);
                influxManager.saveDataToInfluxDB(stringObjectMap);

                response.release();
            }
        }
    }

    /**
     * 응답받은 데이터를 파싱한 Map 을 base64 형식으로 변환
     * @param data
     * @return
     */
    private String convertMapToJsonAndEncodeBase64(Map<String, Object> data) {
        Gson gson = new Gson();
        String jsonData = gson.toJson(data);
        log.info("jsonData: {}", jsonData);

        // JSON 문자열을 바이트 배열로 변환한 후 Base64로 인코딩
        byte[] jsonDataBytes = jsonData.getBytes();
        byte[] base64Data = Base64.getEncoder().encode(jsonDataBytes);

        return new String(base64Data);
    }

}
