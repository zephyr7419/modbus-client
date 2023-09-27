package com.example.ModbusClient.util.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.util.BitVector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ModbusTCP6266 {

    private ModbusTCPMaster master ;

    public void connect() {
        if (master == null || !master.isConnected()) {
            master = new ModbusTCPMaster("172.30.1.5", 502);
        }

        try {
            master.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BitVector readDiCoilValues() {
        try {
            return master.readCoils(0x00000, 4);
        } catch (ModbusException e) {
            log.error("Failed to read coil values: {}", e.getMessage());
            return null;
        }
    }

    public BitVector readDoCoilValues() {
        try {
            return master.readCoils(0x00010, 4);
        } catch (ModbusException e) {
            log.error("Failed to read coil values: {}", e.getMessage());
            return null;
        }
    }

    public void writeCoilFanOn(int value) {
        try {
            master.writeCoil(0x00010, value == 1);
            // 쓰기의 경우 DO 는 0/ 1/ 2/ 3 각 순서대로 주소값은 00010 ~ 00013 까지이다.
        } catch (ModbusException e) {
            log.error("Failed to write coil values: {}", e.getMessage());
        }
    }

    public void writeCoilRemote(boolean flag) {
        try {
            // 쓰기의 경우 DO 는 0/ 1/ 2/ 3 각 순서대로 주소값은 00010 ~ 00013 까지이다.
            master.writeCoil(0x00011, flag);
        } catch (ModbusException e) {
            log.error("Failed to write coil values: {}", e.getMessage());
        }
    }
}
