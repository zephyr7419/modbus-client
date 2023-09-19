package com.example.ModbusClient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownFormat {

    private String houseFanStatus;
    private Boolean mode;
    private Integer fanMotorRPM;
    private String airflowRate;
    private String current;
    private String voltage;
    private String powerConsumption;

}
