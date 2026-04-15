package edge.verdant.controller.machine;

import edge.verdant.pojo.dto.MachineCameraDTO;
import edge.verdant.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController("machineMachineCameraController")
@RequestMapping("/machine/machineCamera")
@Slf4j
@Tag(name = "设备摄影接口")
public class MachineCameraController {
    /**
     * 定期摄影存储
     */
    @PostMapping
    @Operation(summary = "保存单次摄影数据")
    public Result save(HttpServletRequest request) {
        try {
            // 1. 提取请求头中的内容
            String diseaseResult = request.getHeader("diseaseResult");
            if (diseaseResult == null || diseaseResult.isEmpty()) {
                return Result.error("请求头中缺少 diseaseResult");
            }

            String warning = request.getHeader("warning");
            if (warning == null || warning.isEmpty()) {
                return Result.error("请求头中缺少 warning");
            }

            String create_time = request.getHeader("create_time");
            if (create_time == null || create_time.isEmpty()) {
                return Result.error("请求头中缺少 create_time");
            }
            LocalDateTime createTime = LocalDateTime.parse(create_time);

            String id = request.getHeader("id");
            if (id == null || id.isEmpty()) {
                return Result.error("请求头中缺少 id");
            }
            Long machineId = Long.valueOf(id);

            // 2. 提取 Body 中的二进制流
            byte[] image = request.getInputStream().readAllBytes();
            if (image.length == 0) {
                return Result.error("请求体为空");
            }

            log.info("摄影设备：{}，摄影图像数据大小: {} bytes", id, image.length);

            MachineCameraDTO machineCameraDTO = new MachineCameraDTO();
            machineCameraDTO.setMachineId(machineId);
            machineCameraDTO.setImage(image);
            machineCameraDTO.setWarning(warning);
            machineCameraDTO.setCreateTime(createTime);

            return Result.success();
        } catch (Exception e) {
            log.error("处理上传数据失败", e);
            return Result.error("处理上传数据失败: " + e.getMessage());
        }
    }
}
