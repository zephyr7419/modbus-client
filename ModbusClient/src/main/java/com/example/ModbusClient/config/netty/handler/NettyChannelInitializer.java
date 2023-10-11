package com.example.ModbusClient.config.netty.handler;

import com.example.ModbusClient.config.netty.handler.ModbusRTUDecoder;
import com.example.ModbusClient.config.netty.handler.ModbusRTUEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

//    private final HexProtocolClientHandler handler;
    private final HexProtocolClientHandler handler;

    @Override
    protected void initChannel(@NotNull SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        ModbusRTUDecoder decoder = new ModbusRTUDecoder();
        ModbusRTUEncoder encoder = new ModbusRTUEncoder();
        pipeline.addLast(new ReadTimeoutHandler(50, TimeUnit.SECONDS));
        pipeline.addLast(decoder, encoder, handler);
    }
}
