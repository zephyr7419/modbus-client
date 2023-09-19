package com.example.ModbusClient.util.mqtt.uplink;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Component
@ConfigurationProperties(prefix = "services")
@Slf4j
public class ServiceProperties {
    public HashMap properties;

    /**
     * 해당 ServiceType에서 Tag로 지정할 값들을 반환하는 메서드
     * @param serviceType
     * @return
     */
    public List<String> getTags(String serviceType) {
        HashMap tags = (HashMap) ((HashMap) properties.get(serviceType)).get("tags");
        Stream<Map.Entry> entries = tags.entrySet().stream();

        return entries.map((entry -> entry.getValue().toString()))
                .collect(Collectors.toList());

    }

    public HashMap getProtocol(String serviceType) {
        HashMap protocols = (HashMap) ((HashMap) properties.get(serviceType)).get("protocols");

        return protocols;
    }

    public Stream<Map.Entry> getProtocolStream(String serviceType) {
        return getProtocol(serviceType).entrySet().stream();
    }
}
