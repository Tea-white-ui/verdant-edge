package edge.verdant.controller.admin;

import edge.verdant.pojo.dto.EmployeeLoginDTO;
import edge.verdant.pojo.dto.EmployeeRegisterDTO;
import edge.verdant.pojo.entity.Employee;
import edge.verdant.pojo.vo.EmployeeLoginVO;
import edge.verdant.result.Result;
import edge.verdant.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Tag(name = "员工接口")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @Operation(summary = "员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录: {}", employeeLoginDTO);
        EmployeeLoginVO vo = employeeService.login(employeeLoginDTO);
        return Result.success(vo);
    }

    @Operation(summary = "新增员工")
    @PostMapping
    public Result<Void> register(@RequestBody EmployeeRegisterDTO employeeRegisterDTO) {
        log.info("添加员工: {}", employeeRegisterDTO);
        employeeService.register(employeeRegisterDTO);
        return Result.success();
    }
}
