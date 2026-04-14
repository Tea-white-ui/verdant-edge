package edge.verdant.service;

import edge.verdant.pojo.dto.MachineRecordDTO;

public interface MachineRecordService {
    /**
     * 保存单次设备传感器数据
     */
    void save(MachineRecordDTO machineRecordDTO);
}
