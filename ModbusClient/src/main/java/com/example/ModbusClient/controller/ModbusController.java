package com.example.ModbusClient.controller;

import com.example.ModbusClient.service.modbus.ModbusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ModbusController {

    private final ModbusService modbusService;

    @PostMapping("/api/v1/start")
    public void getStart() {
        log.info("Program Start");
    }
}
