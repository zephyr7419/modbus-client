package com.example.ModbusClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ModbusClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModbusClientApplication.class, args);
	}

}
