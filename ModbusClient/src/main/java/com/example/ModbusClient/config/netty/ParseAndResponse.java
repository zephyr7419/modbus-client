package com.example.ModbusClient.config.netty;

import com.google.gson.JsonObject;
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

        Map<String, Object> fields = new HashMap<>();
        int dataStartIndex = 3; // 응답 데이터의 시작 인덱스
        while (dataStartIndex + 1 < response.length) {
            byte hi = response[dataStartIndex];
            byte lo = response[dataStartIndex + 1];
            int value = ((hi & 0xFF) << 8) | (lo & 0xFF);

            String translatedTag = translateToEnglish(dataStartIndex, byteCount);
            String formattedValue = formatData(dataStartIndex, value, byteCount);

            fields.put(translatedTag, formattedValue);
            logParsedData(formattedValue);

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

        logInfoFields(fields);
        return fields;

    }


    public JsonObject mqttMessageToParsing(Map<String, Object> stringObjectMap, String[] statusArray) {
        JsonObject json = new JsonObject();
        json.addProperty("HZ_PV", (String) stringObjectMap.get("Target Frequency"));
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

    private String formatData(int dataStartIndex, int value, byte byteCount) {
        switch (dataStartIndex) {
            case 3, 5, 7, 15 -> {
                if (byteCount != 0x02) {
                    return String.valueOf((value * 0.1));
                } else {
                    return String.valueOf((value * 0.01));
                }
            }
            case 9 -> {
                if (byteCount != 0x02) {
                    return String.valueOf((value * 0.01));
                } else {
                    return null;
                }
            }
            case 11, 13 -> {
                if (byteCount != 0x02) {
                    return String.valueOf(value);
                } else {
                    return null;
                }
            }
            case 17 -> {
                if (byteCount != 0x02) {
                    return getRunStatusString(value);
                } else {
                    return null;
                }
            }
            default -> {
                return Integer.toHexString(value);
            }
        }
    }

    private String translateToEnglish(int dataStartIndex, byte byteCount) {
        switch (dataStartIndex) {
            case 3 -> {
                if (byteCount != 0x02) {
                    return "Acceleration Time";
                } else {
                    return "Target Frequency";
                }
            }
            case 5 -> {
                if (byteCount != 0x02) {
                    return "Deceleration Time";
                } else {
                    return null;
                }
            }
            case 7 -> {
                if (byteCount != 0x02) {
                    return "Output Current";
                } else {
                    return null;
                }
            }
            case 9 -> {
                if (byteCount != 0x02) {
                    return "Output Frequency";
                } else {
                    return null;
                }
            }
            case 11 -> {
                if (byteCount != 0x02) {
                    return "Output Voltage";
                } else {
                    return null;
                }
            }
            case 13 -> {
                if (byteCount != 0x02) {
                    return "DC Link Voltage";
                } else {
                    return null;
                }
            }
            case 15 -> {
                if (byteCount != 0x02) {
                    return "Output Kw";
                } else {
                    return null;
                }
            }
            case 17 -> {
                if (byteCount != 0x02) {
                    return "Operating Status";
                } else {
                    return null;
                }
            }
            default -> {
                return "CRC";
            }
        }
    }

    private void logInfoFields(Map<String, Object> fields) {
        fields.forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
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

//    private String runAndFrequencyResource(int value) {
//        return getRunAndFrequencyString(value);
//    }
//
//    @NotNull
//    public String getRunAndFrequencyString(int value) {
//        int b15to8 = (value >> 8) & 0xF;
//        int b7to0 = value >> 4;
//        String resource = "";
//
//        switch (b15to8) {
//            case 0x00 -> resource = "키패드";
//            case 0x01 -> resource = "통신 옵션";
//            case 0x03 -> resource = "내장형 485";
//            case 0x04 -> resource = "단자대";
//            default -> resource = "Unknown Version";
//        }
//
//        switch (b7to0) {
//            case 0x00 -> resource += ", 키패드 속도";
//            case 0x02, 0x04, 0x03 -> resource += ", Up/Down 운전 속도";
//            case 0x05 -> resource += ", V1";
//            case 0x07 -> resource += ", V2";
//            case 0x08 -> resource += ", I2";
//            case 0x09 -> resource += ", Pulse";
//            case 0x10 -> resource += ", 내장형 485";
//            case 0x11 -> resource += ", 통신 옵션";
//            case 0x13 -> resource += ", Jog";
//            case 0x14 -> resource += ", PID";
//            case 0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31 -> resource += ", 다단속 주파수";
//            default -> resource += ", Unknown Version";
//        }
//
//        return resource;
//    }

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

