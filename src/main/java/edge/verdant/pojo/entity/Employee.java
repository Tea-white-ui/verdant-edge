package edge.verdant.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("employee")
public class Employee {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO) // 指定主键生成策略
    private Long id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户手机号
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
}
