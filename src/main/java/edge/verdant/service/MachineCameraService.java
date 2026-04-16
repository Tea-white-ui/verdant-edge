package edge.verdant.service;

import edge.verdant.pojo.dto.MachineCameraDTO;
import edge.verdant.pojo.entity.MachineCamera;

public interface MachineCameraService {
    /**
     * 保存单次设备摄影数据
     */
    void save(MachineCameraDTO machineCameraDTO);

    /**
     * 根据设备id查询最新设备摄影记录
     */
    MachineCamera getById(Long machineId);
}
