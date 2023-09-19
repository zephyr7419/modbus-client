package com.example.ModbusClient.util;

import com.example.ModbusClient.config.influx.InfluxConfig;
import com.example.ModbusClient.dto.DownFormat;
import com.example.ModbusClient.dto.ParseData;
import com.google.gson.Gson;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class InfluxManager {

    private final InfluxDBClient influxDBClient;
    private WriteApiBlocking writeApi;

    @Value("${spring.influxdb.measurement}")
    private String measurement;

    @PostConstruct
    public void init() {
        writeApi = influxDBClient.getWriteApiBlocking();
    }

    public void saveDataToInfluxDB(Map<String, Object> data) {
        Point point = Point.measurement(measurement).time(System.currentTimeMillis(), WritePrecision.MS);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                point.addField(key, (String) value);
            }
        }

        log.info("success!");
        writeApi.writePoint(point);
    }
}