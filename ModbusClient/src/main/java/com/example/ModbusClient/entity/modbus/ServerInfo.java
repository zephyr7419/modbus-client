package com.example.ModbusClient.entity.modbus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerInfo {
    private String name;
    private String host;
    private int port;
}
