package edge.verdant.controller.admin;

import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.result.Result;
import edge.verdant.service.MachineRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController("adminMachineRecordController")
@Tag(name = "设备记录接口")
@RequestMapping("/machine/machineRecord")
@RequiredArgsConstructor
public class MachineRecordController {
    private final MachineRecordService machineRecordService;
    /**
     * 根据设备id查询最新设备记录
     */
    @GetMapping("/{id}")
    public Result<MachineRecord> getById(@PathVariable("id") Long id){
        MachineRecord machineRecord = machineRecordService.getById(id);
        return Result.success(machineRecord);
    }

    /**
     * 根据用户id批量查询最新设备记录
     */
    @GetMapping("/getByEmployeeIdId")
    public Result<List<MachineRecord>> getByEmployeeIdId(Long userId){
        List<MachineRecord> machineRecords = machineRecordService.getByEmployeeId(userId);
        return Result.success(machineRecords);
    }
}
