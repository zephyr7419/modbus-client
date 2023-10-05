package com.example.ModbusClient.config.netty.handler;

import com.example.ModbusClient.util.InfluxManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Slf4j
public class ModbusRTUDecoder extends ByteToMessageDecoder {

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
}
