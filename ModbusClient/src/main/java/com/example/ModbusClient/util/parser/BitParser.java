package com.example.ModbusClient.util.parser;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public interface BitParser extends Parser{
    default String subBits(String bits, int index, int size) {
        return bits.substring(index, index + size);
    }

    default String bitMapping(String bits, HashMap params) {
        int key = this.bitToInteger(bits);
        return params.get(String.valueOf(key)).toString();
    }

    default Integer bitToInteger(String bits) {
        return Integer.parseInt(bits, 2);
    }

    /**
     * Byte를 Bit로 쪼개어 파싱하는 방법
     */
    default void ParseBits(JsonObject parseData, byte[] payload, HashMap details) {
        // bit들의 파싱 프로토콜
        HashMap bitProtocol = (HashMap) details.get("bit_protocol");
        // byte를 bit payload로 변환
        String bits = byteToBits(payload);
        Stream<Map.Entry> bit_protocol_stream = bitProtocol.entrySet().stream();

        bit_protocol_stream.forEach((entry) -> {
            // parseData에 넣을 Key 값 설정
            String key = (String) entry.getKey();
            // 세부 파싱 내역 가져옴
            HashMap bit_details = (HashMap) entry.getValue();
            // Byte를 index, size 기반으로 자름
            Integer index = (Integer) bit_details.get("index");
            Integer size = (Integer) bit_details.get("size");
            String bitPayload = subBits(bits, index, size);
            // 파싱할 function
            String func = (String) bit_details.get("func");

            // function에 따른 파싱
            switch (func) {
                case "Mapping" -> {
                    HashMap maps = (HashMap) bit_details.get("params");
                    parseData.addProperty(key, maps.get(bitToInteger(bitPayload).toString()).toString());
                    ;
                }
                case "ParseNum" -> parseData.addProperty(key, bitToInteger(bitPayload));
            }
        });
    }
}
