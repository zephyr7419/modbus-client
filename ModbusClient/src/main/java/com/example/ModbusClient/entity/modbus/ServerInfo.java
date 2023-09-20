package com.example.ModbusClient.entity.modbus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerInfo {
    private String name;
    private String host;
    private int port;
}
