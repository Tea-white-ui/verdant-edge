package edge.verdant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edge.verdant.exception.BaseException;
import edge.verdant.mapper.EmployeeMapper;
import edge.verdant.pojo.dto.EmployeeLoginDTO;
import edge.verdant.pojo.dto.EmployeeRegisterDTO;
import edge.verdant.pojo.entity.Employee;
import edge.verdant.pojo.vo.EmployeeLoginVO;
import edge.verdant.properties.JwtProperties;
import edge.verdant.service.EmployeeService;
import edge.verdant.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeMapper employeeMapper;
    private final int STRENGTH = 5; // 哈希迭代参数
    private final JwtProperties jwtProperties;

    /**
     * 员工登录
     */
    @Override
    public EmployeeLoginVO login(EmployeeLoginDTO employeeLoginDTO) {
        // 查询员工是否存在，与加密后的密码比对
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeLoginDTO,employee);

        // 员工输入的密码
        String password = employee.getPassword();

        // 根据员工名查询数据库的代码
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("name",employee.getName());
        employee = employeeMapper.selectOne(wrapper);

        // 验证密码是否匹配
        boolean isMatch = BCrypt.checkpw(password, employee.getPassword());
        if(isMatch){
            // 密码匹配，生成jwt，并返回员工信息
            Map map = new HashMap();
            map.put(employee.getName(),employee.getId());
            employee.setPassword("******");
            String jwt = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(),
                    jwtProperties.getAdminTtl(),
                    map);
            EmployeeLoginVO employeeLoginVO = new EmployeeLoginVO();
            BeanUtils.copyProperties(employee,employeeLoginVO);
            employeeLoginVO.setToken(jwt);
            return employeeLoginVO;
        }else {
            // 密码或员工名错误，抛出异常
            throw new BaseException("密码或员工名错误");
        }
    }

    /**
     * 添加员工
     */
    @Override
    public void register(EmployeeRegisterDTO employeeRegisterDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeRegisterDTO, employee);
        // 哈希密码
        String hashPassword = hashPassword(employee.getPassword());
        employee.setPassword(hashPassword);
        // 插入员工到数据库
        employeeMapper.insert(employee);
    }

    /**
     * 哈希密码
     */
    private String hashPassword(String password){
        PasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);
        return encoder.encode(password);
    }

}
