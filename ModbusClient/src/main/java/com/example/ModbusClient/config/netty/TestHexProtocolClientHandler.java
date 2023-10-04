package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.entity.modbus.ModbusRequestProperties;
import com.example.ModbusClient.service.ModbusServiceTest;
import com.example.ModbusClient.util.modbus.ModbusProtocol;
import com.example.ModbusClient.util.modbus.ModbusTCP6266;
import com.example.ModbusClient.util.modbus.Request;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@ChannelHandler.Sharable
public class TestHexProtocolClientHandler extends ChannelInboundHandlerAdapter {

    private final ModbusServiceTest modbusService;
    private final ModbusRequestProperties properties;
    private final ModbusTCP6266 tcp6266;
    private final ModbusProtocol protocol;
    private final Request request;

    public TestHexProtocolClientHandler(ModbusServiceTest modbusService, ModbusRequestProperties properties, ModbusTCP6266 tcp6266, ModbusProtocol protocol, Request request) {
        this.modbusService = modbusService;
        this.properties = properties;
        this.tcp6266 = tcp6266;
        this.protocol = protocol;
        this.request = request;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
//        ctx.pipeline().addLast(new ReadTimeoutHandler(25, TimeUnit.SECONDS));
//        modbusService.heartbeatRequest();

    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
//        ctx.pipeline().remove(ReadTimeoutHandler.class);
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        byte[] response = (byte[]) msg;

        if (response[1] == 6) {
            ctx.executor().schedule(() -> {
                try {
                    modbusService.writeResponseParsing(response, ctx);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, 5, TimeUnit.SECONDS);

        }else if (response[2] == 4) {
            modbusService.onSecondResponseReceived(response, ctx);

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
        log.error("Exception caught in TestHexProtocolClientHandler:", cause);

        if (cause instanceof ReadTimeoutException) {
            log.warn("데이터 수신 타임아웃 발생, 재요청 시작!!");
//            ctx.channel().eventLoop().execute(() -> {
//                try {
////                    modbusService.heartbeatRequest();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            });
        } else {
            log.error("예외 발생: {}", cause.getMessage());
            cause.printStackTrace();
        }

        log.info("Calling super.exceptionCaught");
        super.exceptionCaught(ctx, cause); // Add this line
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("읽기 종료");
    }
}
