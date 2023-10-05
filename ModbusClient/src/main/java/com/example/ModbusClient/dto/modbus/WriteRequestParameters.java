package com.example.ModbusClient.dto.modbus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WriteRequestParameters {
    private int startAddress;
    private double value;
}
