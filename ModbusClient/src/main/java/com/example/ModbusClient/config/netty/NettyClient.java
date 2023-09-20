package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.config.ServerConfig;
import com.example.ModbusClient.entity.modbus.ServerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class NettyClient {

    private final Bootstrap bootstrap;
    private Channel channel;
    private final ServerConfig serverConfig;

    public void start() {
        List<ServerInfo> servers = serverConfig.getServerList();
        for (ServerInfo serverInfo : servers) {
            connectToServer(serverInfo.getHost(), serverInfo.getPort());
        }
    }

    private void connectToServer(String host, int port) {
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                log.info("Connected to server: {}:{}", host, port);
            } else {
                log.error("Failed to server: {}:{}", host, port);
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
                start();
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
