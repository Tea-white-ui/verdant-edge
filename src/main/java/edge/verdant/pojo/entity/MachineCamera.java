package edge.verdant.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("machine_camera")
public class MachineCamera {
    private Long id;
    private Long machineId;
    private String image;
    private String diseaseResult;
    private String warning;
    private LocalDateTime createTime;
}
