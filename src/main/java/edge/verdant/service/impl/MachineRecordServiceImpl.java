package edge.verdant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edge.verdant.exception.BaseException;
import edge.verdant.mapper.MachineMapper;
import edge.verdant.mapper.MachineRecordMapper;
import edge.verdant.pojo.dto.MachineOldRecordsDTO;
import edge.verdant.pojo.dto.MachineRecordDTO;
import edge.verdant.pojo.entity.Machine;
import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.service.MachineRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineRecordServiceImpl implements MachineRecordService {
    private final MachineRecordMapper machineRecordMapper;
    private final MachineMapper machineMapper;

    /**
     * 保存单次设备传感器数据
     */
    @Override
    public void save(MachineRecordDTO machineRecordDTO) {
        MachineRecord machineRecord = new MachineRecord();
        BeanUtils.copyProperties(machineRecordDTO, machineRecord);
        machineRecord.setMachineId(machineRecordDTO.getId());
        machineRecord.setId(null);
        // 查询数据库判断设备id是否存在
        Machine machine = machineMapper.selectById(machineRecord.getMachineId());
        if (machine != null) {
            // 如果存在则允许记录
            machineRecordMapper.insert(machineRecord);
        }else {
            // 设备未注册，抛出异常
            throw new BaseException("设备未注册");
        }
    }

    /**
     * 根据设备id查询最新记录
     */
    @Override
    public MachineRecord getById(Long id) {
        // 构建查询条件，根据设备ID查询
        LambdaQueryWrapper<MachineRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MachineRecord::getMachineId, id)
                .orderByDesc(MachineRecord::getCreateTime) // 假设按创建时间倒序排列以获取最新记录
                .last("LIMIT 1"); // 只取最新的一条
        
        // 执行查询并返回结果
        return machineRecordMapper.selectOne(queryWrapper);
    }

    /**
     * 根据员工id批量获得最新的设备记录
     */
    @Override
    public List<MachineRecord> getByEmployeeId(Long employeeId) {
        // 1. 根据员工ID查询其关联的所有设备ID
        LambdaQueryWrapper<Machine> machineQueryWrapper = new LambdaQueryWrapper<>();
        machineQueryWrapper.eq(Machine::getId, employeeId);
        List<Machine> machines = machineMapper.selectList(machineQueryWrapper);

        if (machines == null || machines.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 提取所有设备ID
        List<Long> machineIds = machines.stream()
                .map(Machine::getId)
                .toList();

        // 3. 查询这些设备的最新记录
        // 注意：MyBatis-Plus 的 selectOne 不支持批量，selectList 配合排序和分组或子查询较复杂。
        // 这里采用一种常见策略：先查出这些设备的所有记录，按时间倒序，然后在内存中去重取每个设备的最新一条。
        // 或者，如果数据量不大，可以直接查询所有相关记录并处理。
        
        LambdaQueryWrapper<MachineRecord> recordQueryWrapper = new LambdaQueryWrapper<>();
        recordQueryWrapper.in(MachineRecord::getMachineId, machineIds)
                .orderByDesc(MachineRecord::getCreateTime);
        
        List<MachineRecord> allRecords = machineRecordMapper.selectList(recordQueryWrapper);

        // 4. 在内存中为每个设备保留最新的一条记录
        // 使用 LinkedHashMap 保持插入顺序（即最新记录的顺序），key 为 machineId
        java.util.Map<Long, MachineRecord> latestRecordMap = new java.util.LinkedHashMap<>();
        for (MachineRecord record : allRecords) {
            // 如果 map 中还没有该 machineId 的记录，则放入（因为已经按 createTime 倒序排列，第一条即为最新）
            latestRecordMap.putIfAbsent(record.getMachineId(), record);
        }

        return new ArrayList<>(latestRecordMap.values());
    }

    /**
     * 根据设备id查询历史数据
     */
    @Override
    public List<MachineRecord> getOldRecordsById(MachineOldRecordsDTO dto) {
        // 构建查询条件，根据设备ID查询历史记录
        LambdaQueryWrapper<MachineRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MachineRecord::getMachineId, dto.getId())
                .orderByDesc(MachineRecord::getCreateTime); // 按创建时间倒序排列

        // 设置分页大小，如果size为空或小于等于0，则默认查询10条，防止查询过多数据
        int size = (dto.getSize() != 0 && dto.getSize() > 0) ? dto.getSize() : 10;
        queryWrapper.last("LIMIT " + size);

        // 执行查询并返回结果列表
        return machineRecordMapper.selectList(queryWrapper);

    }
}
