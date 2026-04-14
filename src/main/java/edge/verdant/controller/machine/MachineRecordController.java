package edge.verdant.controller.machine;

import edge.verdant.pojo.dto.MachineRecordDTO;
import edge.verdant.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("machineMachineRecordController")
@Tag(name = "设备传感器记录接口")
@RequestMapping("/machine/machineRecord")
public class MachineRecordController {
    @PostMapping()

    @Operation(summary = "保存设备传感器记录")
    public Result save(MachineRecordDTO machineRecordDTO){
        log.info("记录设备传感器数据：{}",machineRecordDTO);
        return Result.success();
    }
}
