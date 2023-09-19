package com.example.ModbusClient.config.modbus;

import com.example.ModbusClient.entity.DeviceResponseData;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Builder
@Getter
@Configuration
public class DeviceResponseDataConfig {

    @Bean
    public DeviceResponseData deviceResponseData() {
        return DeviceResponseData.builder().build();
    }
}
