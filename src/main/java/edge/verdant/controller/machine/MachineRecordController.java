package edge.verdant.controller.machine;

import edge.verdant.pojo.dto.MachineRecordDTO;
import edge.verdant.result.Result;
import edge.verdant.service.MachineRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("machineMachineRecordController")
@Tag(name = "设备传感器记录接口")
@RequestMapping("/machine/machineRecord")
@RequiredArgsConstructor
public class MachineRecordController {
    private final MachineRecordService machineRecordService;

    @PostMapping
    @Operation(summary = "保存单词传感器记录")
    public Result save(@RequestBody MachineRecordDTO machineRecordDTO){
        log.info("记录设备传感器数据：{}",machineRecordDTO);
        machineRecordService.save(machineRecordDTO);
        return Result.success();
    }
}
