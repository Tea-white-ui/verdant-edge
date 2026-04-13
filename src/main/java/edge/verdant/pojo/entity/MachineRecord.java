package edge.verdant.pojo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class MachineRecord {
    /**
     * 主键
     */
    private Long id;

    /**
     * 设备 id（外键）
     */
    private Long machineId;

    /**
     * 土壤湿度 (%)
     */
    private BigDecimal soilMoisture;

    /**
     * 土壤温度 (°C)
     */
    private BigDecimal soilTemp;

    /**
     * 空气温度 (°C)
     */
    private BigDecimal airTemp;

    /**
     * 空气湿度 (%)
     */
    private BigDecimal airHumidity;

    /**
     * 二氧化碳浓度 (ppm)
     */
    private BigDecimal co2;

    /**
     * 光照强度 (Lux)
     */
    private BigDecimal lightIntensity;

    /**
     * 成长阶段
     */
    private Integer growthStage;

    /**
     * 病害结果
     */
    private String diseaseResult;

    /**
     * 摄影图像
     */
    private String imageUrl;

    /**
     * 警告内容
     */
    private String warning;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
