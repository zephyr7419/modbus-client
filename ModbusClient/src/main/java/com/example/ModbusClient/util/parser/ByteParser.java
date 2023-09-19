package com.example.ModbusClient.util.parser;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;

public interface ByteParser extends Parser{
    default byte[] subBytes(byte[] bytes, int index, int size) {
        return Arrays.copyOfRange(bytes, index, index + size);
    }
    default String byteToHex(byte[] bytes) {
        String byteString = "";
        int var4 = bytes.length;

        for (byte b : bytes) {
            int decimal = b & 255;
            String hex = Integer.toHexString(decimal).toUpperCase();
            byteString = byteString.concat(hex.length() == 1 ? "0" + hex : hex);
        }

        return byteString;
    }

    default Long parseUnsignedLong(byte[] payload) {
        String hex = byteToHex(payload);

        return Long.parseLong(hex, 16);
    }

    default Long parseSignedLong(byte[] payload) {
        String hex = byteToHex(payload);
        int byteLen = hex.length()/2;
        int bitLen = byteLen * 8 - 1;

        long maxValue = (long) Math.pow(2, bitLen) - 1;
        long decimalValue = Long.parseLong(hex, 16);

        if (decimalValue > maxValue) {
            decimalValue -= (1L << (hex.length() * 4));
        }

        return decimalValue;
    }

    default String byteMapping(byte[] payload, HashMap params) {
        Long key = parseUnsignedLong(payload);
        return params.get(key.toString()).toString();
    }

    default Long parseLong(byte[] payload, String policy) {
        Long result = null;

        if(policy.equals("Long")) {
            result = parseSignedLong(payload);
        } else if (policy.equals("ULong")) {
            result = parseUnsignedLong(payload);
        }

        return result;
    }

    default BigDecimal calOffset(Long parseNum, HashMap calc) {
        BigDecimal result = BigDecimal.valueOf(parseNum);

        if(calc.containsKey("scaling")) {
            BigDecimal scaling = new BigDecimal((calc.get("scaling")).toString()) ;
            result = result.multiply(scaling);
        }
        if(calc.containsKey("offset")) {
            BigDecimal offset = new BigDecimal((calc.get("offset")).toString()) ;
            result = result.add(offset);
        }

        return result;
    }

    default String byteToBits(byte[] bytes) {
        String bits = "";
        byte[] var3 = bytes;
        int var4 = bytes.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            byte b = var3[var5];
            bits = bits.concat(String.format("%8s", Integer.toBinaryString(b & 255)).replace(' ', '0'));
        }

        return bits;
    }

    /**
     * Num 방식의 파싱 방법
     * SignedLong, UnSignedLong인지 판별
     * 추가적인 Scaling, Offset 계산 진행
     */
    default void ParseNum(JsonObject parseData, String key, byte[] payload, HashMap details) {
        Long parseNum = parseUnsignedLong(payload);
        String hexPayload = byteToHex(payload);

        // F로만 이루어진 데이터는 해당 obd 메뉴에서 나올수 없는 데이터이기에 Undefiend로 처리
        if(details.containsKey("obd") && hexPayload.matches("[F]+")) {
            parseData.addProperty(key, 9999999999L);
        } else {
            // 추가 계산을 해야할 때
            if(details.containsKey("calc")) {
                HashMap calc = (HashMap) details.get("calc");
                parseData.addProperty(key, calOffset(parseNum, calc));
            }
            // 추가 계산이 필요없을 때
            else {
                parseData.addProperty(key, parseUnsignedLong(payload));
            }
        }
    }

    /**
     * Bit Flag 방식의 파싱 방법
     */
    default void ParseBitFlag(JsonObject parseData, byte[] payload, HashMap details) {
        HashMap flags = (HashMap) details.get("flags");
        String bits = byteToBits(payload);
        JsonObject result = new JsonObject();

        // bit들을 돌며 1인 Flag에 SET, 0인 Flag에 UNSET
        for(int i=0; i<bits.length(); i++) {
            String key = (String) flags.get(String.valueOf(i));

            if(bits.charAt(i) == '1') {
                parseData.addProperty(key, '1');
            } else {
                parseData.addProperty(key, '0');
            }
        }
    }

    default void ParseGPS(JsonObject parsingData, String key, byte[] payload, HashMap details) {
        String policy = details.get("policy").toString();
        Long parseNum = parseLong(payload, policy);

        parsingData.addProperty(key, readWtfToDeg(parseNum));
    }

    private BigDecimal readWtfToDeg(Long data) {
        int deg = (int) Math.floor((double) data / 100_000);
        int min = (int) (data % 100_000);
        String coord = deg + "." + String.format("%05d", min);
        return convertLatLngFromDegMinToDeg(coord);
    }

    private BigDecimal convertLatLngFromDegMinToDeg(String coordinate) {
        double deg = (int) Math.floor(Double.parseDouble(coordinate) / 100);
        double min = (Double.parseDouble(coordinate) % 100) / 60;
        double result = Double.parseDouble(String.format("%.6f", deg + min));

        return BigDecimal.valueOf(result);
    }
}
