package edge.verdant.service.impl;


import edge.verdant.mapper.MachineCameraMapper;
import edge.verdant.pojo.dto.MachineCameraDTO;
import edge.verdant.pojo.entity.MachineCamera;
import edge.verdant.service.MachineCameraService;
import edge.verdant.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MachineCameraServiceImpl implements MachineCameraService {
    private final AliOssUtil aliOssUtil;
    private final MachineCameraMapper machineCameraMapper;

    @Override
    public void save(MachineCameraDTO machineCameraDTO) {
        // 上传图片到oss，文件名用UUID命名
        byte[] image = machineCameraDTO.getImage();
        String url = aliOssUtil.upload(image, "machine-camera/" + UUID.randomUUID() + "/" + machineCameraDTO.getCreateTime() + ".jpg");

        // 将数据添加到数据库
        MachineCamera machineCamera = new MachineCamera();
        BeanUtils.copyProperties(machineCameraDTO,machineCamera);
        machineCamera.setImage_url(url);
        machineCameraMapper.insert(machineCamera);

        // 病害判断
        int diseaseResult = machineCamera.getDiseaseResult();
        if(diseaseResult == 1 || diseaseResult == 2){
            // todo 植被异常，发出警报
        }

    }
}
