package com.example.ModbusClient.config.netty;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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

        if (byteCount == 0x04) {
            Map<String, Object> fields2 = new HashMap<>();
            int dataStartIndex = 3; // 응답 데이터의 시작 인덱스
            while (dataStartIndex + 1 < response.length) {
                byte hi = response[dataStartIndex];
                byte lo = response[dataStartIndex + 1];
                int value = ((hi & 0xFF) << 8) | (lo & 0xFF);

                String translatedTag = translateToEnglish2(dataStartIndex);
                String formattedValue = formatData2(dataStartIndex, value);

                fields2.put(translatedTag, formattedValue);
                logParsedData(formattedValue);

                dataStartIndex += 2;
            }

            // 나머지 파라미터와 CRC 출력
            return getStringObjectMap(response, slaveId, funcCode, byteCount, fields2);
        } else if (byteCount == 16) {
            Map<String, Object> fields = new HashMap<>();
            int dataStartIndex = 3; // 응답 데이터의 시작 인덱스
            while (dataStartIndex + 1 < response.length) {
                byte hi = response[dataStartIndex];
                byte lo = response[dataStartIndex + 1];
                int value = ((hi & 0xFF) << 8) | (lo & 0xFF);

                String translatedTag = translateToEnglish8(dataStartIndex);
                String formattedValue = formatData8(dataStartIndex, value);

                fields.put(translatedTag, formattedValue);
                logParsedData(formattedValue);

                dataStartIndex += 2;
            }

            // 나머지 파라미터와 CRC 출력
            return getStringObjectMap(response, slaveId, funcCode, byteCount, fields);

//        } else if (funcCode == 10){
//            Map<String, Object> writeValueFields = new HashMap<>();
//
//            byte addrHi = (byte) ((response[2] >> 8) & 0xFF);
//            byte addrLo = (byte) (response[2] & 0xFF);
//            byte NoHi = (byte) ((response[3] >> 8) & 0xFF);
//            byte NoLo = (byte) (response[3] & 0xFF);
//            byte byteCounts = response[4];
//            byte crcLo = (byte) (response[4] & 0xFF);
//            byte crcHi = (byte) ((response[4] >> 8) & 0xFF);
//
//            writeValueFields.put("slaveId", slaveId);
//            writeValueFields.put("funcCode", funcCode);
//            writeValueFields.put("NoHi", NoHi);
//            writeValueFields.put("NoLo", NoLo);
//            writeValueFields.put("byteCount", byteCounts )
//            writeValueFields.put("valueHi", valueHi);
//            writeValueFields.put("valueLo", valueLo);
//            writeValueFields.put("crcLo", crcLo);
//            writeValueFields.put("crcHi", crcHi);
//
//            return writeValueFields;
//        } else {
        } else {
            log.info("write data: {}", Arrays.toString(response));
            Map<String, Object> exceptionResponse = new HashMap<>();

            byte exceptCode = response[3];

            switch (exceptCode) {
                case 0x01 ->
                    log.error("ILLEGAL FUNCTION");
                case 0x02 ->
                    log.error("ILLEGAL DATA ADDRESS");
                case 0x03 ->
                    log.error("ILLEGAL DATA VALUE");
                case 0x06 ->
                    log.error("SLAVE DEVICE BUSY");
                case 0x14 ->
                    log.error("Write-Protection");
            }

            exceptionResponse.put("exceptCode", exceptCode);
            return exceptionResponse;
        }
    }

    @NotNull
    private Map<String, Object> getStringObjectMap(byte[] response, byte slaveId, byte funcCode, byte byteCount, Map<String, Object> fields2) {
        byte crcLo = response[response.length - 2];
        byte crcHi = response[response.length - 1];
        fields2.put("Slave ID", slaveId);
        fields2.put("Function Code", funcCode);
        fields2.put("Byte Count", byteCount);
        fields2.put("CRC (Lo)", crcLo);
        fields2.put("CRC (Hi)", crcHi);

        logInfoFields(fields2);
        return fields2;
    }

    private void logInfoFields(Map<String, Object> fields) {
        fields.forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
    }

    public JsonObject mqttMessageToParsing(Map<String, Object> stringObjectMap, String[] statusArray) {
        JsonObject json = new JsonObject();
        json.addProperty("FWD_RUN_ST", statusArray[3].equals("True"));
        json.addProperty("REV_RUN_ST", statusArray[2].equals("True"));
        json.addProperty("STOP_ST", statusArray[4].equals("True"));
        json.addProperty("TRIP_ST", statusArray[1].equals("True"));
        json.addProperty("INC_TIME", (String) stringObjectMap.get("Acceleration Time"));
        json.addProperty("DEC_TIME", (String) stringObjectMap.get("Deceleration Time"));
        json.addProperty("OUT_A", (String) stringObjectMap.get("Output Current"));
        json.addProperty("OUT_HZ", (String) stringObjectMap.get("Output Frequency"));
        json.addProperty("OUT_V", (String) stringObjectMap.get("Output Voltage"));
        json.addProperty("DC_LINK_V", (String) stringObjectMap.get("DC Link Voltage"));
        json.addProperty("KW", (String) stringObjectMap.get("Output Kw"));
        json.addProperty("REMOTE", statusArray[0].equals("HAND") ? 0 : 1);
        json.addProperty("INV_POWER_ST", (String) stringObjectMap.get("Deceleration Time"));
        return json;
    }

    private void logParsedData(String formattedValue) {

        log.info(formattedValue);
    }

    private String formatData2(int dataStartIndex, int value) {
        switch (dataStartIndex) {
            case 3 -> {
                return String.valueOf((value * 0.01));
            }
            default -> {
                return Integer.toHexString(value);
            }
        }
    }

    private String formatData8(int dataStartIndex, int value) {
        switch (dataStartIndex) {
            case 3, 5, 7, 15 -> {
                return String.valueOf((value * 0.1));
            }
            case 9 -> {
                return String.valueOf((value * 0.01));
            }
            case 11, 13 -> {
                return String.valueOf(value);
            }
            case 17 -> {
                return getRunStatusString(value);
            }
            default -> {
                return Integer.toHexString(value);
            }
        }
    }

    private String translateToEnglish2(int dataStartIndex) {
        switch (dataStartIndex) {
            case 3 -> {
                return "Target Frequency";
            }
            default -> {
                return "CRC";
            }
        }
    }

    private String translateToEnglish8(int dataStartIndex) {
        switch (dataStartIndex) {
            case 3 -> {
                return "Acceleration Time";
            }
            case 5 -> {
                return "Deceleration Time";
            }
            case 7 -> {
                return "Output Current";
            }
            case 9 -> {
                return "Output Frequency";
            }
            case 11 -> {
                return "Output Voltage";
            }
            case 13 -> {
                return "DC Link Voltage";
            }
            case 15 -> {
                return "Output Kw";
            }
            case 17 -> {
                return "Operating Status";
            }
            default -> {
                return "CRC";
            }
        }
    }

    private String translateToEnglish10(int dataStartIndex) {
        switch (dataStartIndex) {
            case 3-> {
                return "Target Frequency";
            }
            case 7 -> {
                return "Acceleration Time";
            }
            case 9 -> {
                return "Deceleration Time";
            }
            case 11 -> {
                return "Output Current";
            }
            case 13 -> {
                return "Output Frequency";
            }
            case 15 -> {
                return "Output Voltage";
            }
            case 17 -> {
                return "DC Link Voltage";
            }
            case 19 -> {
                return "Output Kw";
            }
            case 21 -> {
                return "Operating Status";
            }
            default -> {
                return "CRC";
            }
        }
    }


    @NotNull
    public String getRunStatusString(int value) {
        int b15 = ((value >> 15) & 1);
        boolean b3 = ((value >> 3) & 1) == 1;
        boolean b2 = ((value >> 2) & 1) == 1;
        boolean b1 = ((value >> 1) & 1) == 1;
        boolean b0 = (value & 1) == 1;

        String status = "";

        switch (b15) {
            case 0x0000 -> status += "HAND";
            case 0x0001 -> status += "AUTO";
        }

        if (b3) {
            status += ",True";
        } else {
            status += ",False";
        }

        if (b2) {
            status += ",True";
        } else {
            status += ",False";
        }

        if (b1) {
            status += ",True";
        } else {
            status += ",False";
        }

        if (b0) {
            status += ",True";
        } else {
            status += ",False";
        }

        return status;
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

