//package com.example.ModbusClient.service.mqtt;
//
//import com.example.ModbusClient.dto.ParseData;
//import com.example.ModbusClient.util.InfluxManager;
//import com.example.ModbusClient.util.exception.ServiceTypeNotMatchingException;
//import com.example.ModbusClient.util.mqtt.uplink.UplinkParser;
//import com.google.gson.JsonParseException;
//import com.google.protobuf.InvalidProtocolBufferException;
//import com.influxdb.exceptions.InfluxException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.eclipse.paho.mqttv5.common.MqttMessage;
//import org.springframework.stereotype.Service;
//
////@Service
//@Slf4j
//@RequiredArgsConstructor
//public class UpEventService {
//
//    private final UplinkParser uplinkParser;
//
//    public void parsing(byte[] data) {
//        try {
////            ParseData parseData = uplinkParser.uplinkParsing(message);
//
//
//        } catch (InvalidProtocolBufferException e) {
//            log.info("Not Uplink Message");
//        } catch (ServiceTypeNotMatchingException e) {
//
//        } catch (InfluxException e) {
//            log.error("Error Occured during saving in Influx DB : {}", e.getMessage());
//            e.printStackTrace();
//        } catch (JsonParseException e) {
//            log.error("Error Occured during parsing Json Obeject : {}", e.getMessage());
//            e.printStackTrace();
//        } catch (Exception e) {
//            log.error("UnExpected Error Occured : {}", e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
