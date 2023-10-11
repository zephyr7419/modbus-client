package com.example.ModbusClient.dto.modbus;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WriteRequestParameters {
    private int parameterCount;
    private int startAddress;
    private int value;
    private List<Integer> values;
}
