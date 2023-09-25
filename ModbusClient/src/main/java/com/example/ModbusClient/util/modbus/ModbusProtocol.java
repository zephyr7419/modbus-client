package com.example.ModbusClient.util.modbus;

import com.example.ModbusClient.entity.modbus.ReadRequestParameters;
import com.example.ModbusClient.entity.modbus.WriteRequestParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModbusProtocol {

    public Function<ReadRequestParameters, byte[]> getReadRequest = readRequestParameters -> {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 1);
        buffer.put((byte) 4);
        buffer.put((byte) ((readRequestParameters.getStartAddress() >> 8) & 0xFF));
        buffer.put((byte) (readRequestParameters.getStartAddress()  & 0xFF));
        buffer.put((byte) ((readRequestParameters.getQuantity() >> 8) & 0xFF));
        buffer.put((byte) (readRequestParameters.getQuantity()  & 0xFF));

        byte[] bytes = calculateCRC16Modbus(buffer.array());
        ByteBuffer finalBuffer = ByteBuffer.allocate(8);
        finalBuffer.order(ByteOrder.BIG_ENDIAN);
        finalBuffer.put(buffer.array());
        finalBuffer.put(bytes[0]);
        finalBuffer.put(bytes[1]);
        log.info("buffer: {}", byteArrayToHexString(finalBuffer.array()));
        return finalBuffer.array();
    };

    public Function<WriteRequestParameters, byte[]> getWriteRequest = writeRequestParameters -> {
        ByteBuffer buffer = ByteBuffer.allocate(7 + writeRequestParameters.getParameterCount() * 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 1);
        buffer.put((byte) 0x10);
        buffer.put((byte) ((writeRequestParameters.getStartAddress() >> 8) & 0xFF));
        buffer.put((byte) (writeRequestParameters.getStartAddress()  & 0xFF));
        buffer.put((byte) ((writeRequestParameters.getParameterCount() >> 8) & 0xFF));
        buffer.put((byte) (writeRequestParameters.getParameterCount() & 0xFF));
        buffer.put((byte) (writeRequestParameters.getParameterCount() * 2));

        for (int value : writeRequestParameters.getValues()) {
            buffer.put((byte) ((value >> 8) & 0xFF));
            buffer.put((byte) (value & 0xFF));
        }

        byte[] bytes = calculateCRC16Modbus(buffer.array());
        ByteBuffer finalBuffer = ByteBuffer.allocate(9 + writeRequestParameters.getParameterCount() * 2);
        finalBuffer.order(ByteOrder.BIG_ENDIAN);
        finalBuffer.put(buffer.array());
        finalBuffer.put(bytes[0]);
        finalBuffer.put(bytes[1]);
        log.info("buffer: {}", byteArrayToHexString(finalBuffer.array()));
        return finalBuffer.array();
    };

    /**
     * CRC-16 을 만들어주는 메서드
     * @param data
     * @return
     */
    private byte[] calculateCRC16Modbus(byte[] data) {
        int crc = 0xFFFF;
        int polynomial = 0xA001;

        for (byte b : data) {
            crc ^= (int) b & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ polynomial;
                } else {
                    crc >>= 1;
                }
            }
        }

        return new byte[]{(byte) (crc & 0xFF), (byte) (crc >> 8)};
    }


}
