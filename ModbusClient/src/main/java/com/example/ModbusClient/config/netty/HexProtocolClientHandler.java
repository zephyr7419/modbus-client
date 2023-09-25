package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.service.ModbusService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;


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
        modbusService.heartbeatRequest();
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        byte[] response = (byte[]) msg;

        if (response[2] == 16) {
            modbusService.onFirstResponseReceived(response, ctx);

        }else if (response[2] == 4) {
            modbusService.onSecondResponseReceived(response, ctx);

        }else if (response[1] == 16){
            modbusService.writeResponseParsing(response);
        } else {
            log.info("알 수 없는 정보이다.");

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
