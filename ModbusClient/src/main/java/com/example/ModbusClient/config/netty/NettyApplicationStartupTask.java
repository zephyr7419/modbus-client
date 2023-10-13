package com.example.ModbusClient.config.netty;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NettyApplicationStartupTask implements ApplicationListener<ApplicationReadyEvent> {

    private final NettyClient client;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
//        client.start();
    }
}
