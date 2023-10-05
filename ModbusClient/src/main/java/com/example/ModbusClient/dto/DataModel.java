package com.example.ModbusClient.dto;

import com.example.ModbusClient.dto.modbus.ServerInfo;
import com.google.gson.annotations.SerializedName;

@lombok.Data
public class DataModel {
    @SerializedName("deviceInfo")
    private ServerInfo deviceInfo;
    @SerializedName("data")
    private Data data;
}
