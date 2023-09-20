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

    public BitVector readCoilValues() {
        try {
            return master.readCoils(0x00000, 4);
        } catch (ModbusException e) {
            log.error("Failed to read coil values: {}", e.getMessage());
            return null;
        }
    }
}
