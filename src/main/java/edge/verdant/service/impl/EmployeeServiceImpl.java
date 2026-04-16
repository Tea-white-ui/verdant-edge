package edge.verdant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edge.verdant.constant.JwtClaimsConstant;
import edge.verdant.exception.BaseException;
import edge.verdant.mapper.EmployeeMapper;
import edge.verdant.pojo.dto.EmployeeLoginDTO;
import edge.verdant.pojo.dto.EmployeePageQueryDTO;
import edge.verdant.pojo.dto.EmployeeRegisterDTO;
import edge.verdant.pojo.entity.Employee;
import edge.verdant.pojo.vo.EmployeeLoginVO;
import edge.verdant.properties.JwtProperties;
import edge.verdant.service.EmployeeService;
import edge.verdant.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
            Map map = new HashMap();
            map.put(JwtClaimsConstant.EMP_ID, employee.getId());
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
     * 分页查询员工
     */
    public Page<Employee> getPage(EmployeePageQueryDTO employeePageQueryDTO) {
        // 1. 创建分页对象
        Page<Employee> page = new Page<>(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        // 2. 构建查询条件（Lambda 写法推荐，编译期安全）
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(employeePageQueryDTO.getKeyword()),
                Employee::getName, employeePageQueryDTO.getKeyword());

        // 3. 执行分页查询
        Page<Employee> resultPage = employeeMapper.selectPage(page, wrapper);

        // 4. 将返回结果中的密码字段脱敏
        if (resultPage != null && resultPage.getRecords() != null) {
            resultPage.getRecords().forEach(emp -> emp.setPassword("******"));
        }

        return resultPage;
    }

    /**
     * 根据id查询员工
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if(employee == null) return null;
        employee.setPassword("******");
        return employee;
    }

    /**
     * 哈希密码
     */
    private String hashPassword(String password){
        PasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);
        return encoder.encode(password);
    }

}
