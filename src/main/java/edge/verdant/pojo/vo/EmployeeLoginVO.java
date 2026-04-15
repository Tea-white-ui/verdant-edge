package edge.verdant.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
public class EmployeeLoginVO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 员工名
     */
    private String name;

    /**
     * 账户密码
     */
    private String password;

    /**
     * 员工邮箱
     */
    private String email;

    /**
     * 员工手机号
     */
    private String phone;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String number;
    /**
     * token
     */
    private String token;
}
