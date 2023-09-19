package com.example.ModbusClient.config.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class NettyClient {

    private static final String SERVER_HOST = "172.30.1.233";
    private static final int SERVER_PORT = 5300;
    private final Bootstrap bootstrap;
    private Channel channel;

    public void start() {
        connectToServer();
    }

    private void connectToServer() {
        ChannelFuture channelFuture = bootstrap.connect(SERVER_HOST, SERVER_PORT);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                log.info("Connected to server: {}:{}", SERVER_HOST, SERVER_PORT);
            } else {
                log.error("Failed to server: {}:{}", SERVER_HOST, SERVER_PORT);
                channel.closeFuture().addListener((ChannelFutureListener) future1 -> {
                    log.warn("Connection closed. Attempting to reconnect...");
                    attemptReconnect();
                });
            }
        });

    }

    private void attemptReconnect() {
        final long reconnectDelay = 5000;

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                connectToServer();
            } catch (Exception e) {
                log.error("Reconnection failed: {}", e.getMessage());
            }
        }, 0, reconnectDelay, TimeUnit.MICROSECONDS);
    }

    @PreDestroy
    public void stop() {
        if (channel != null) {
            channel.close();
            channel.parent().closeFuture();
        }
    }
}
