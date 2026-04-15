package edge.verdant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edge.verdant.pojo.dto.EmployeeLoginDTO;
import edge.verdant.pojo.dto.EmployeePageQueryDTO;
import edge.verdant.pojo.dto.EmployeeRegisterDTO;
import edge.verdant.pojo.entity.Employee;
import edge.verdant.pojo.vo.EmployeeLoginVO;

import java.util.List;

public interface EmployeeService {
    /**
     * 登录接口
     */
    EmployeeLoginVO login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 添加员工
     */
    void register(EmployeeRegisterDTO employeeRegisterDTO);

    /**
     * 分页查询员工
     */
    Page<Employee> getPage(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 根据id查询员工
     */
    Employee getById(Long id);
}
