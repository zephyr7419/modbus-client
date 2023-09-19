package com.example.ModbusClient.service.mqtt;

import com.example.ModbusClient.util.mqtt.downlink.DownLinkSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//@Service
@RequiredArgsConstructor
@Slf4j
public class DownEventService {
    private final DownLinkSender downLinkSender;
}
