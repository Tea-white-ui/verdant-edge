package edge.verdant.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MachineCameraDTO {
    /**
     * 设备id
     */
    private Long machineId;
    /**
     * 摄影图片
     */
    private byte[] image;
    /**
     * 诊断结果
     */
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
