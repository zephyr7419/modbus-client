package com.example.ModbusClient.config.netty;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ParseAndResponse {

    /**
     *
     * @param response
     * @return
     */
    public Map<String, Object> parseAndLogResponse(byte[] response) {
        byte slaveId = response[0];
        byte funcCode = response[1];
        byte byteCount = response[2];
        int dataStartIndex = 3; // 응답 데이터의 시작 인덱스

        Map<String, Object> fields = new HashMap<>();

        // 응답 데이터를 파싱하여 로그에 출력
        while (dataStartIndex + 1 < response.length) {
            byte hi = response[dataStartIndex];
            byte lo = response[dataStartIndex + 1];
            int value = ((hi & 0xFF) << 8) | (lo & 0xFF);

            if (byteCount == 0x04) {
                String translatedTag = translateToEnglishFrom4Count(dataStartIndex);
                String formattedValue = formatData(dataStartIndex, value);
                fields.put(translatedTag, formattedValue);
                logParsedData1(dataStartIndex, value);
            } else {
                String translatedTag = translateToEnglishFromOtherCount(dataStartIndex);
                String formattedValue = formatData2(dataStartIndex, value);
                fields.put(translatedTag, formattedValue);
                logParsedData2(dataStartIndex, value);
            }

            // 데이터 인덱스를 2바이트씩 증가시킴
            dataStartIndex += 2;
        }

        // 나머지 파라미터와 CRC 출력
        byte crcLo = response[response.length - 2];
        byte crcHi = response[response.length - 1];
        fields.put("Slave ID", slaveId);
        fields.put("Function Code", funcCode);
        fields.put("Byte Count", byteCount);
        fields.put("CRC (Lo)", crcLo);
        fields.put("CRC (Hi)", crcHi);
        log.info("Slave ID: " + slaveId);
        log.info("Function Code: " + funcCode);
        log.info("Byte Count: " + byteCount);
        log.info("CRC (Lo): " + crcLo);
        log.info("CRC (Hi): " + crcHi);

        return fields;
    }

    private void logParsedData1(int dataStartIndex, int value) {
        String formattedValue = formatData(dataStartIndex, value);
        log.info(formattedValue);
    }

    private void logParsedData2(int dataStartIndex, int value) {
        String formattedValue = formatData2(dataStartIndex, value);
        log.info(formattedValue);
    }

    private String formatData(int dataStartIndex, int value) {
        String formattedValue = "";
        switch (dataStartIndex) {
            case 3 -> formattedValue = runStatus(value);
            case 5 -> formattedValue = runAndFrequencyResource(value);
            default -> formattedValue = Integer.toHexString(value);
        }
        return formattedValue;
    }

    private String formatData2(int dataStartIndex, int value) {
        String formattedValue = "";
        switch (dataStartIndex) {
            case 3, 5 -> formattedValue = ((value * 0.1) + "sec");
            case 7 -> formattedValue = ((value * 0.1) + "A");
            case 9 -> formattedValue = ((value * 0.01) + "Hz");
            case 11 -> formattedValue = (value + "V");
            default -> formattedValue = Integer.toHexString(value);
        }
        return formattedValue;
    }

    private String translateToEnglishFrom4Count(int dataStartIndex) {
        return switch (dataStartIndex) {
            case 3 -> "Operating Status";
            case 5 -> "Operation and Frequency Command Source";
            default -> "CRC";
        };
    }

    private String translateToEnglishFromOtherCount(int dataStartIndex) {
        return switch (dataStartIndex) {
            case 3 -> "Acceleration Time";
            case 5 -> "Deceleration Time";
            case 7 -> "Output Current";
            case 9 -> "Output Frequency";
            case 11 -> "Output Voltage";
            default -> "CRC";
        };
    }

    private String runStatus(int value) {
        // S/W 버전을 계산하여 반환
        return getRunStatusString(value);
    }

    @NotNull
    public String getRunStatusString(int value) {
        int b15to12 = (value >> 12) & 0xF;
        int b7to4 = (value >> 4) & 0xF;
        int b3to0 = value & 0xF;
        String status = "";

        switch (b15to12) {
            case 0x00 -> status = "정상 상태";
            case 0x04 -> status = "Warning 발생 상태";
            case 0x08 -> status = "Fault 발생 상태";
            default -> status = "Unknown Version";
        }

        switch (b7to4) {
            case 0x01 -> status += ", 속도 서치 중";
            case 0x02 -> status += ", 가속 중";
            case 0x03 -> status += ", 정속 중";
            case 0x04 -> status += ", 감속 중";
            case 0x05 -> status += ", 감속 정지 중";
            case 0x06 -> status += ", H/W 전류 억제";
            case 0x07 -> status += ", S/W 전류 억제";
            case 0x08 -> status += ", 드웰 운전 중";
            default -> status += ", Unknown Version";
        }

        switch (b3to0) {
            case 0x00 -> status += ", 정지";
            case 0x01 -> status += ", 정방향 운전 중";
            case 0x02 -> status += ", 역방향 운전 중";
            case 0x03 -> status += ", DC 운전 중";
            default -> status += ", Unknown Version";
        }

        return status;
    }

    private String runAndFrequencyResource(int value) {
        return getRunAndFrequencyString(value);
    }

    @NotNull
    public String getRunAndFrequencyString(int value) {
        int b15to8 = (value >> 8) & 0xF;
        int b7to0 = value >> 4;
        String resource = "";

        switch (b15to8) {
            case 0x00 -> resource = "키패드";
            case 0x01 -> resource = "통신 옵션";
            case 0x03 -> resource = "내장형 485";
            case 0x04 -> resource = "단자대";
            default -> resource = "Unknown Version";
        }

        switch (b7to0) {
            case 0x00 -> resource += ", 키패드 속도";
            case 0x02, 0x04, 0x03 -> resource += ", Up/Down 운전 속도";
            case 0x05 -> resource += ", V1";
            case 0x07 -> resource += ", V2";
            case 0x08 -> resource += ", I2";
            case 0x09 -> resource += ", Pulse";
            case 0x10 -> resource += ", 내장형 485";
            case 0x11 -> resource += ", 통신 옵션";
            case 0x13 -> resource += ", Jog";
            case 0x14 -> resource += ", PID";
            case 0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31 -> resource += ", 다단속 주파수";
            default -> resource += ", Unknown Version";
        }

        return resource;
    }

    /**
     * log 확인 시 16진수 표현
     * @param msg
     * @return
     */
    public static String byteArrayToHexString(byte[] msg) {
        StringBuilder sb = new StringBuilder();
        for (byte b : msg) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}

