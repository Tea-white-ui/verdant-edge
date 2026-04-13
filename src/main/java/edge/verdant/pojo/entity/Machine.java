package edge.verdant.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("machine")

public class Machine {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO) // 指定主键生成策略
    private Long id;
    /**
     * 员工id
     */
    private Long employee_id;
    /**
     * 设备名称
     */
    private String name;
    /**
     * 设备类型
     */
    private int type;
    /**
     * 设备编号
     */
    private int number;
    /**
     * 植物名称
     */
    private String plant_name;
    /**
     * 植物类型
     */
    private String plant_type;
    /**
     * 创建时间
     */
    private LocalDateTime create_time;
    /**
     * 修改时间
     */
    private LocalDateTime update_time;
}
