package com.example.ModbusClient.config;

import com.example.ModbusClient.entity.modbus.ServerInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter @Setter
@ConfigurationProperties(prefix = "servers")
public class ServerConfig {
    private List<ServerInfo> serverList;
}
