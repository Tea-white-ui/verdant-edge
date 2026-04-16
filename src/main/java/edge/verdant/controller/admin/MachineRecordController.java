package edge.verdant.controller.admin;

import edge.verdant.pojo.dto.MachineOldRecordsDTO;
import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.result.Result;
import edge.verdant.service.MachineRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("adminMachineRecordController")
@Tag(name = "设备记录接口")
@RequestMapping("/admin/machineRecord")
@RequiredArgsConstructor
public class MachineRecordController {
    private final MachineRecordService machineRecordService;
    /**
     * 根据设备id查询最新设备记录
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据设备id查询最新")
    public Result<MachineRecord> getById(@PathVariable Long id){
        MachineRecord machineRecord = machineRecordService.getById(id);
        return Result.success(machineRecord);
    }

    /**
     * 根据用户id批量查询最新设备记录
     */
    @Operation(summary = "根据用户id批量查询")
    @GetMapping("/getByEmployeeIdId")
    public Result<List<MachineRecord>> getByEmployeeIdId(Long userId){
        List<MachineRecord> machineRecords = machineRecordService.getByEmployeeId(userId);
        return Result.success(machineRecords);
    }
    /**
     * 根据设备id查询历史数据
     */
    @PostMapping("/getOldRecordsById")
    @Operation(summary = "根据设备id查询历史")
    public Result<List<MachineRecord>> getOldRecordsById(@RequestBody MachineOldRecordsDTO dto){
        List<MachineRecord> machineRecords = machineRecordService.getOldRecordsById(dto);
        return Result.success(machineRecords);
    }

}
