package com.example.ModbusClient.util.parser;

public interface Parser {
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
}
