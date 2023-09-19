package com.example.ModbusClient.entity;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeviceResponseData {
    private byte[] payload;
}
