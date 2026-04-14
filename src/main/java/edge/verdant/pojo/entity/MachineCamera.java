package edge.verdant.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("machine_camera")
public class MachineCamera {
    /**
     * 主键
     */
    private Long id;
    /**
     * 设备id
     */
    private Long machineId;
    /**
     * 摄影图片
     */
    private String image;
    /**
     * 诊断结果
     */
    private String diseaseResult;
    /**
     * 警告内容
     */
    private String warning;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
