package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.util.InfluxManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.util.*;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;


@RequiredArgsConstructor
@Slf4j
public class ModbusRTUDecoder extends ByteToMessageDecoder {

    private final InfluxManager influxManager;
    // 이전 데이터를 저장하는 변수
    private final Map<String, Object> previousData = new HashMap<>();
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
                byte[] responseData = new byte[response.readableBytes()];
                response.readBytes(responseData);
                log.info("responseData: {}", Arrays.toString(responseData));
                out.add(responseData);

                response.release();
            }
        }
    }

    private Map<String, Object> getStringObjectMap(byte[] responseData) throws MqttException {
        ParseAndResponse parseAndLogResponse = new ParseAndResponse();
        Map<String, Object> stringObjectMap = parseAndLogResponse.parseAndLogResponse(responseData);
        sendMqttMessage(stringObjectMap, parseAndLogResponse);
        log.info("Received Modbus RTU Response : {}", byteArrayToHexString(responseData));

        return stringObjectMap;
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

    private void sendMqttMessage(Map<String, Object> stringObjectMap, ParseAndResponse parseAndResponse) {

        String statusString = (String) stringObjectMap.get("Operating Status");
        String[] statusArray = statusString.split(",");
        JsonObject jsonObject = parseAndResponse.mqttMessageToParsing(stringObjectMap, statusArray);

        String jsonDataBase64 = convertMapToJsonAndEncodeBase64(jsonObject);

        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttConnectionOptions connectionOptions = new MqttConnectionOptions();
            MqttClient mqttClient = new MqttClient("tcp://localhost:1883", "zephyr", persistence);
            mqttClient.connect(connectionOptions);

            String topic = "application/test";
            MqttMessage message = new MqttMessage(jsonDataBase64.getBytes());
            message.setQos(0);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }



}
