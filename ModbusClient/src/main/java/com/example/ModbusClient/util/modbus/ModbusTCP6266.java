package com.example.ModbusClient.util.modbus;

import com.example.ModbusClient.entity.DeviceResponseData;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.util.BitVector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

            if (master.isConnected()) {

                BitVector coils = new BitVector(4);
                BitVector d1Value = master.readCoils(0x00000, 4);

                if (!d1Value.getBit(0)) {
                    coils.setBit(0, false);
                    master.writeMultipleCoils(1, 16, coils);
                } else if (d1Value.getBit(0)) {
                    coils.setBit(0, true);
                    master.writeMultipleCoils(1, 16, coils);
                }
                BitVector bitVector = master.readCoils(16, 4);
                log.info("register: {}", d1Value);
                DeviceResponseData.builder()
                                .payload(bitVector.getBytes())
                                .build();

                log.info("DO value: {}", bitVector);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
