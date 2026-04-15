package edge.verdant.service;

import edge.verdant.pojo.dto.MachineCameraDTO;

public interface MachineCameraService {
    /**
     * 保存单次设备摄影数据
     */
    void save(MachineCameraDTO machineCameraDTO);
}
