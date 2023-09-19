package com.example.ModbusClient.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public class FormattingProtocol {
    private static final Map<Integer, Function<Integer, String>> STATUS_FORMATTERS = new HashMap<>();
    private static final Map<Integer, Function<Integer, String>> SPEED_STATUS_FORMATTERS = new HashMap<>();
    private static final Map<Integer, Function<Integer, String>> DIRECTION_STATUS_FORMATTERS = new HashMap<>();
    private static final Map<Integer, Function<Integer, String>> DRIVING_COMMAND_SOURCE = new HashMap<>();
    private static final Map<Integer, Function<Integer, String>> FREQUENCY_COMMAND_SOURCE = new HashMap<>();


    static {
        // 상태(Status) 포맷터 초기화
        STATUS_FORMATTERS.put(0, v -> "정상");
        STATUS_FORMATTERS.put(4, v -> "Warning");
        STATUS_FORMATTERS.put(8, v -> "Fault");
        // 다른 용량에 대한 처리 추가

        // 속도 상태(Speed Status) 포맷터 초기화
        SPEED_STATUS_FORMATTERS.put(1, v -> "속도 서치 중");
        SPEED_STATUS_FORMATTERS.put(2, v -> "가속 중");
        SPEED_STATUS_FORMATTERS.put(3, v -> "정속 중");
        SPEED_STATUS_FORMATTERS.put(4, v -> "감속 중");
        SPEED_STATUS_FORMATTERS.put(5, v -> "감속 정지 중");
        SPEED_STATUS_FORMATTERS.put(6, v -> "H/W 전류 억제");
        SPEED_STATUS_FORMATTERS.put(7, v -> "S/W 전류 억제");
        SPEED_STATUS_FORMATTERS.put(8, v -> "드웰 운전중");
        // 다른 전압/전원 형태에 대한 처리 추가

        // 방향 상태(Direction Status) 포맷터 초기화
        DIRECTION_STATUS_FORMATTERS.put(0, v -> "정지");
        DIRECTION_STATUS_FORMATTERS.put(1, v -> "정방향 운전중");
        DIRECTION_STATUS_FORMATTERS.put(2, v -> "역방향 운전중");
        DIRECTION_STATUS_FORMATTERS.put(3, v -> "DC 운전 중");
        // 다른 버전에 대한 처리 추가

        DRIVING_COMMAND_SOURCE.put(0, v-> "키패드");
        DRIVING_COMMAND_SOURCE.put(1, v-> "통신 옵션");
        DRIVING_COMMAND_SOURCE.put(3, v-> "내장형 485");
        DRIVING_COMMAND_SOURCE.put(4, v-> "단자대");

        FREQUENCY_COMMAND_SOURCE.put(0, v-> "키패드 속도");
        FREQUENCY_COMMAND_SOURCE.put(2, v-> "Up/Down 운전 속도");
        FREQUENCY_COMMAND_SOURCE.put(3, v-> "Up/Down 운전 속도");
        FREQUENCY_COMMAND_SOURCE.put(4, v-> "Up/Down 운전 속도");
        FREQUENCY_COMMAND_SOURCE.put(5, v-> "V1");
        FREQUENCY_COMMAND_SOURCE.put(7, v-> "V2");
        FREQUENCY_COMMAND_SOURCE.put(8, v-> "I2");
        FREQUENCY_COMMAND_SOURCE.put(9, v-> "Pulse");
        FREQUENCY_COMMAND_SOURCE.put(10, v-> "내장형 485");
        FREQUENCY_COMMAND_SOURCE.put(11, v-> "통신옵션");
        FREQUENCY_COMMAND_SOURCE.put(13, v-> "Jog");
        FREQUENCY_COMMAND_SOURCE.put(14, v-> "PID");
    }

    public static String formatStatus(int value) {
        return STATUS_FORMATTERS.getOrDefault(value, Integer::toHexString).apply(value);
    }

    public static String formatSpeedStatus(int value) {
        return SPEED_STATUS_FORMATTERS.getOrDefault(value, Integer::toHexString).apply(value);
    }

    public static String formatDirectionStatus(int value) {
        return DIRECTION_STATUS_FORMATTERS.getOrDefault(value, Integer::toHexString).apply(value);
    }

    public static String formatDrivingCommandSource(int value) {
        return DRIVING_COMMAND_SOURCE.getOrDefault(value, Integer::toHexString).apply(value);
    }

    public static String formatFrequencyCommandSource(int value) {
        return FREQUENCY_COMMAND_SOURCE.getOrDefault(value, Integer::toHexString).apply(value);
    }


}
