package com.example.ModbusClient.dto;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParseData {
    private JsonObject data;
}
