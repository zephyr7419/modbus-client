package com.example.ModbusClient.dto;

import com.google.gson.annotations.SerializedName;


@lombok.Data
public class Data {
    @SerializedName("HZ_SV")
    private String hzSv;

    @SerializedName("FAN_ON")
    private int fanOn;
}
