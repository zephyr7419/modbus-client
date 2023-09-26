package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.dto.DataModel;
import com.example.ModbusClient.service.ModbusServiceTest;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

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
        if (message != null) {
            log.info("message: {}", message);
        }
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        byte[] response = (byte[]) msg;

        if (response[1] == 16) {
            modbusService.writeResponseParsing(response, ctx);

            Thread.sleep(5000);

        }else if (response[2] == 4) {
            Thread.sleep(1000);
            modbusService.onSecondResponseReceived(response, ctx);
            log.info("shouldExecuteC: {}", shouldExecuteC);
            Thread.sleep(10000);
            executeA();

        }else if (response[2] == 16){
            log.info("정보 받아오나?");
            Thread.sleep(1000);
            modbusService.onFirstResponseReceived(response, ctx);
            Thread.sleep(5000);
            modbusService.heartbeatRequest();
        } else {
            log.info("알 수 없는 정보이다.");

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

    public void getPayload(DataModel dataModel) throws InterruptedException {
        if (dataModel != null) {
            Gson gson = new Gson();
            message = gson.toJson(dataModel);
            log.info("여기까지옴?");
            log.info("message:{}", message);

            modbusService.writeRequest(dataModel);
            executeA();
        } else {
            log.info("없음");
        }

    }

    private void executeA() throws InterruptedException {
        Thread.sleep(1000);
        setShouldExecuteC(checkForMqttMessage());
        if (shouldExecuteC) {
            executeC();
        } else {
            log.info("executeA 통과?");
            executeB();
        }
    }

    private void executeC() throws InterruptedException {
        log.info("message: {}", message);
        Thread.sleep(1000);
        modbusService.heartbeatRequest();
    }

    private void executeB() throws InterruptedException {
        modbusService.connectAndRequest();
    }

    private void setShouldExecuteC(boolean b) {
        this.shouldExecuteC = b;
    }

    private boolean checkForMqttMessage() {
        return message != null;
    }
}
