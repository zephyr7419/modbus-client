package com.example.ModbusClient.util.exception;

public class ServiceTypeNotMatchingException extends RuntimeException{
    public ServiceTypeNotMatchingException(String devEui) {
        super("This Device [" + devEui + "]Not Matched Service Type");
    }
}
