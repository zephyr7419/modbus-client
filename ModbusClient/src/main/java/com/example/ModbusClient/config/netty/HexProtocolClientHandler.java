package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.service.ModbusService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@ChannelHandler.Sharable
public class HexProtocolClientHandler extends ChannelInboundHandlerAdapter {

    private final ModbusService modbusService;
    private final Map<ChannelHandlerContext, List<Map<String, Object>>> parsedResponsesMap = new ConcurrentHashMap<>();
    private Map<String, Object> stringObjectMap = new ConcurrentHashMap<>();
    private Map<String, Object> combinedMap = new ConcurrentHashMap<>();

    public HexProtocolClientHandler(ModbusService modbusService) {
        this.modbusService = modbusService;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        modbusService.removeServer(ctx);
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        byte[] response = (byte[]) msg;


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
