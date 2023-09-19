package com.example.ModbusClient.entity;

public class VirtualDevice {
    private String name;
    private String host;
    private int port;

    public byte[] readData() {
        return new byte[]{0x01, 0x04, 0x06};
    }
}
