package edge.verdant.service.impl;

import edge.verdant.exception.BaseException;
import edge.verdant.mapper.MachineMapper;
import edge.verdant.mapper.MachineRecordMapper;
import edge.verdant.pojo.dto.MachineRecordDTO;
import edge.verdant.pojo.entity.Machine;
import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.service.MachineRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MachineRecordServiceImpl implements MachineRecordService {
    private final MachineRecordMapper machineRecordMapper;
    private final MachineMapper machineMapper;

    /**
     * 保存单词设备传感器数据
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
            // 警告内容可以赛水分过低，或温度过高什么的
            // todo 判断传感器数据是否异常，如果异常则发出警告通知该员工
            machineRecordMapper.insert(machineRecord);
        }else {
            // 设备未注册，抛出异常
            throw new BaseException("设备未注册");
        }
    }
}
