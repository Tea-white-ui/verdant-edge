package edge.verdant.service;

import edge.verdant.pojo.entity.Machine;

import java.util.List;

public interface MachineService {
    /**
     * 根据id查询设备信息
     */
    Machine getById(Long id);

    /**
     * 根据员工id批量查询设备信息
     */
    List<Machine> getByEmployeeId(Long employeeId);

    /**
     * 根据设备id判断设备是否在线
     */
    Integer isOnline(Long id);
}
