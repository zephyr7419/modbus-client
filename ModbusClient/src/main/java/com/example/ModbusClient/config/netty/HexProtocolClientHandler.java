package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.service.ModbusService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import static com.example.ModbusClient.config.netty.ParseAndResponse.byteArrayToHexString;


@Slf4j
@Component
@ChannelHandler.Sharable
public class HexProtocolClientHandler extends ChannelInboundHandlerAdapter {

    private final ModbusService modbusService;

    public HexProtocolClientHandler(ModbusService modbusService) {
        this.modbusService = modbusService;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        modbusService.addServer(ctx);
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        modbusService.removeServer(ctx);
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] response = new byte[buf.readableBytes()];
        buf.readBytes(response);


        log.info("response: {}", byteArrayToHexString(response));
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
