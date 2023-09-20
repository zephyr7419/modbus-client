package com.example.ModbusClient.entity.modbus;

import lombok.Data;

@Data
public class ReadRequestParameters {
    private int startAddress;
    private int quantity;
}
