package com.example.ModbusClient.util.parser;

import java.util.ArrayList;
import java.util.List;

public interface NumToByteParser {
    default List<Byte> convertOneByte(int num) {
        List<Byte> result = new ArrayList<>();
        result.add((byte) num);

        return result;
    }
    default List<Byte> convertFourByte(long num) {
        List<Byte> result = new ArrayList<>();

        result.add((byte) (num >> 24));
        result.add((byte) (num >> 16));
        result.add((byte) (num >> 8));
        result.add((byte) num);

        return result;
    }
    default List<Byte> convertAscII(String str) {
        List<Byte> result = new ArrayList<>();
        for(char c: str.toCharArray()) {
            result.addAll(convertOneByte(c));
        }

        return result;
    }

//    default List<Byte> convertBoolean(boolean bol) {
//        List<Byte> result = new ArrayList<>();
//        result.add(bol);
//    }
//    default List<Byte> convertMCodeList(List<MaintainanceCode> mCodeList) {
//        List<Byte> result = new ArrayList<>();
//        for(MaintainanceCode mCode: mCodeList) {
//            result.addAll(convertOneByte(mCode.getValue()));
//        }
//        return result;
//    }
}
