package com.example.ModbusClient.config.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxConfig {
    @Value("${spring.influxdb.url}")
    private String url;
    @Value("${spring.influxdb.user}")
    private String user;
    @Value("${spring.influxdb.password}")
    private String password;
    @Value("${spring.influxdb.org}")
    private String org;
    @Value("${spring.influxdb.bucket}")
    private String bucket;
    @Value("${spring.influxdb.token}")
    private String token;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}
