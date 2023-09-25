package com.example.ModbusClient.entity.modbus;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WriteRequestParameters {
    private int parameterCount;
    private int startAddress;
    private List<Integer> values;
}
