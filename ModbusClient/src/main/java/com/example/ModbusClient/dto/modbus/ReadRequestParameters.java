package com.example.ModbusClient.dto.modbus;

import lombok.Data;

@Data
public class ReadRequestParameters {
    private int startAddress;
    private int quantity;
}
