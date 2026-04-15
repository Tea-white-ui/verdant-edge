package edge.verdant.service;

import edge.verdant.pojo.dto.EmployeeLoginDTO;
import edge.verdant.pojo.dto.EmployeeRegisterDTO;
import edge.verdant.pojo.vo.EmployeeLoginVO;

public interface EmployeeService {
    /**
     * 登录接口
     */
    EmployeeLoginVO login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 添加员工
     */
    void register(EmployeeRegisterDTO employeeRegisterDTO);
}
