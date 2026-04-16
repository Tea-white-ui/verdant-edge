package edge.verdant.controller.admin;

import edge.verdant.pojo.entity.Machine;
import edge.verdant.result.Result;
import edge.verdant.service.MachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.ReusableMessage;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminMachineController")
@RequestMapping("/admin/machine")
@Slf4j
@Tag(name = "设备接口")
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;

    /**
     * 根据id查询设备信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询设备信息")
    public Result<Machine> getById(@PathVariable Long id){
        Machine machine = machineService.getById(id);
        return Result.success(machine);
    }

    /**
     * 根据员工id批量查询设备信息
     */
    @GetMapping("/getByUserId/{EmployeeId}")
    @Operation(summary = "根据员工id批量查询设备信息")
    public Result<List<Machine>> getByEmployeeId(@PathVariable Long EmployeeId) {
        List<Machine> machines = machineService.getByEmployeeId(EmployeeId);
        return Result.success(machines);
    }

    /**
     * 根据设备id查询设备是否在线
     */
    @PostMapping("/isOnline")
    @Operation(summary = "根据设备id查询设备是否在线")
    public Result<Integer> isOnline(Long id){
        Integer result = machineService.isOnline(id);
        return Result.success(result);
    }
}
