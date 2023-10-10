package com.example.ModbusClient.config.netty.handler;

import com.example.ModbusClient.service.modbus.ModbusService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class HexProtocolClientHandler extends ChannelInboundHandlerAdapter {

    private final ModbusService modbusService;

    private long startTime;

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
        Thread.sleep(5);
        byte[] response = (byte[]) msg;

        if (response[1] == 6) {
            log.info("쓰기 후 정보 읽기");
            startTime = System.currentTimeMillis();
//            Thread.sleep(5);
            modbusService.writeResponseParsing(response, ctx);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            log.info("작업 수행 시간 (밀리초): {}", elapsedTime);

        }else if (response[2] == 4) {
            startTime = System.currentTimeMillis();
//            Thread.sleep(5);
            modbusService.onSecondResponseReceived(response);

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            log.info("작업 수행 시간 (밀리초): {}", elapsedTime);

        } else if (response[2] == 8) {
            startTime = System.currentTimeMillis();
//            Thread.sleep(5);
            modbusService.onThirdResponseReceived(response, ctx);

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            log.info("작업 수행 시간 (밀리초): {}", elapsedTime);
        } else if (response[2] == 16){
            startTime = System.currentTimeMillis();
//            Thread.sleep(5);
            modbusService.onFirstResponseReceived(response, ctx);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            log.info("작업 수행 시간 (밀리초): {}", elapsedTime);
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
