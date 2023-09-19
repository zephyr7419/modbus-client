package com.example.ModbusClient.util.modbus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadRequestParameters {
    private int startAddress;
    private int quantity;
}
