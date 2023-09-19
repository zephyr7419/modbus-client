package com.example.ModbusClient.util.modbus;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WriteRequestParameters {

    private int parameterCount;
    private int startAddress;
    private List<Integer> values;
}
