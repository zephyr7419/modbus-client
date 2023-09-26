package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.util.InfluxManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

//    private final HexProtocolClientHandler handler;
    private final InfluxManager influxManager;
    private final TestHexProtocolClientHandler handler;

    @Override
    protected void initChannel(@NotNull SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        ModbusRTUDecoder decoder = new ModbusRTUDecoder();
        ModbusRTUEncoder encoder = new ModbusRTUEncoder();
        pipeline.addLast(decoder, encoder, handler);
    }
}
