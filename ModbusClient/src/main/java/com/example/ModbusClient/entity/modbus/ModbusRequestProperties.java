package com.example.ModbusClient.entity.modbus;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter @Setter
@ConfigurationProperties(prefix = "modbus")
public class ModbusRequestProperties {
    private List<WriteRequestParameters> writeRequests;
    private List<ReadRequestParameters> readRequests;
}
