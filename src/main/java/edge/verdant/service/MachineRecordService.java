package edge.verdant.service;

import edge.verdant.pojo.dto.MachineRecordDTO;
import edge.verdant.pojo.entity.MachineRecord;

import java.util.List;

public interface MachineRecordService {
    /**
     * 保存单次设备传感器数据
     */
    void save(MachineRecordDTO machineRecordDTO);

    /**
     * 根据设备id查询最新记录
     */
    MachineRecord getById(Long id);

    /**
     * 根据员工id批量获得最新设备记录
     */
    List<MachineRecord> getByEmployeeId(Long employeeId);
}

