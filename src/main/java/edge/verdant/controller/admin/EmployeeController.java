package edge.verdant.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edge.verdant.pojo.dto.EmployeeLoginDTO;
import edge.verdant.pojo.dto.EmployeePageQueryDTO;
import edge.verdant.pojo.dto.EmployeeRegisterDTO;
import edge.verdant.pojo.entity.Employee;
import edge.verdant.pojo.vo.EmployeeLoginVO;
import edge.verdant.result.PageResult;
import edge.verdant.result.Result;
import edge.verdant.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Scanner;

@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Tag(name = "员工接口")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    /**
     * 员工登录接口
     */
    @Operation(summary = "员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录: {}", employeeLoginDTO);
        EmployeeLoginVO vo = employeeService.login(employeeLoginDTO);
        return Result.success(vo);
    }

    /**
     * 新增员工接口
     */
    @Operation(summary = "新增员工")
    @PostMapping
    public Result<Void> register(@RequestBody EmployeeRegisterDTO employeeRegisterDTO) {
        log.info("添加员工: {}", employeeRegisterDTO);
        employeeService.register(employeeRegisterDTO);
        return Result.success();
    }
    /**
     * 分页查询员工接口
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询员工")
    public PageResult getPage(@RequestBody EmployeePageQueryDTO dto) {
        Page<Employee> page = employeeService.getPage(dto);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(page.getRecords());
        return pageResult;
    }

    /**
     * 根据id查询员工信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询员工")
    public Result<Employee> getById(@PathVariable("id") Long id){
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }
}
