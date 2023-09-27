package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.service.ModbusServiceTest;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;


@Slf4j
@Component
@ChannelHandler.Sharable
public class TestHexProtocolClientHandler extends ChannelInboundHandlerAdapter {

    private final ModbusServiceTest modbusService;
    private Queue<String> tasks = new ArrayDeque<>();
    private boolean isProcessing = false;
    private boolean shouldExecuteC = false;
    private String message = null;

    public TestHexProtocolClientHandler(ModbusServiceTest modbusService) {
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

        if (response[1] == 6) {
            Thread.sleep(5000);
            modbusService.writeResponseParsing(response, ctx);

        }else if (response[2] == 4) {
            modbusService.onSecondResponseReceived(response, ctx);
            log.info("shouldExecuteC: {}", shouldExecuteC);

        } else if (response[2] == 8) {
            modbusService.onThirdResponseReceived(response, ctx);

        } else if (response[2] == 16){
            modbusService.onFirstResponseReceived(response, ctx);
        } else {
            log.info("알 수 없는 정보이다.");

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("읽기 종료");
    }
}
