package edge.verdant.service.impl;


import edge.verdant.mapper.EmployeeMapper;
import edge.verdant.mapper.MachineCameraMapper;
import edge.verdant.mapper.MachineMapper;
import edge.verdant.pojo.dto.MachineCameraDTO;
import edge.verdant.pojo.entity.Employee;
import edge.verdant.pojo.entity.Machine;
import edge.verdant.pojo.entity.MachineCamera;
import edge.verdant.service.EmailService;
import edge.verdant.service.MachineCameraService;
import edge.verdant.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineCameraServiceImpl implements MachineCameraService {
    private final AliOssUtil aliOssUtil;
    private final MachineCameraMapper machineCameraMapper;
    private final EmailService emailService;
    private final MachineMapper machineMapper;
    private final EmployeeMapper employeeMapper;

    private static final Map<String, LocalDateTime> EMAIL_SEND_RECORDS = new ConcurrentHashMap<>();

    /**
     * 保存单次摄影图片
     */

    @Override
    public void save(MachineCameraDTO machineCameraDTO) {
        byte[] image = machineCameraDTO.getImage();
        String url = aliOssUtil.upload(image, "machine-camera/" + UUID.randomUUID() + "/" + machineCameraDTO.getCreateTime() + ".jpg");

        MachineCamera machineCamera = new MachineCamera();
        BeanUtils.copyProperties(machineCameraDTO,machineCamera);
        machineCamera.setImage_url(url);
        machineCameraMapper.insert(machineCamera);

        int diseaseResult = machineCamera.getDiseaseResult();
        if(diseaseResult == 1 || diseaseResult == 2){
            Machine machine = machineMapper.selectById(machineCamera.getMachineId());
            Employee employee = employeeMapper.selectById(machine.getEmployeeId());
            
            String alertKey = employee.getEmail() + ":" + machineCamera.getMachineId();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastSendTime = EMAIL_SEND_RECORDS.get(alertKey);
            
            if (lastSendTime == null || lastSendTime.plusDays(1).isBefore(now)) {
                emailService.sendSimpleMail(employee.getEmail(), "植被异常警报", "设备ID：" + machineCamera.getMachineId() + " 植被异常");
                EMAIL_SEND_RECORDS.put(alertKey, now);
                log.info("发送植被异常警报邮件 - 邮箱: {}, 设备ID: {}", employee.getEmail(), machineCamera.getMachineId());
            } else {
                log.info("跳过重复警报邮件 - 邮箱: {}, 设备ID: {}, 上次发送时间: {}", employee.getEmail(), machineCamera.getMachineId(), lastSendTime);
            }
        }

    }

    /**
     * 根据设备id查询最新设备摄影记录
     */
    @Override
    public MachineCamera getById(Long machineId) {
        return machineCameraMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MachineCamera>()
                        .eq(MachineCamera::getMachineId, machineId)
                        .orderByDesc(MachineCamera::getCreateTime)
                        .last("LIMIT 1")
        );
    }
}
