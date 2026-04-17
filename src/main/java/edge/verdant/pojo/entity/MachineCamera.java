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
    private String imageUrl;
    /**
     * 诊断结果
     */
    // 0为正常，1为黄叶异常，2为白斑异常
    private int diseaseResult;
    /**
     * 警告内容
     */
    private String warning;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
