package edge.verdant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edge.verdant.mapper.MachineMapper;
import edge.verdant.pojo.entity.Machine;
import edge.verdant.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {
    private final MachineMapper machineMapper;
    /**
     * 根据id查询设备信息
     */
    @Override
    public Machine getById(Long id) {
        return machineMapper.selectById(id);
    }

    /**
     * 根据员工id批量查询设备信息
     */
    @Override
    public List<Machine> getByEmployeeId(Long employeeId) {
        LambdaQueryWrapper<Machine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Machine::getEmployeeId, employeeId);
        return machineMapper.selectList(queryWrapper);
    }

    /**
     * 根据设备id查询设备是否在线
     */
    @Override
    public Integer isOnline(Long id) {
        return 1;
    }
}
