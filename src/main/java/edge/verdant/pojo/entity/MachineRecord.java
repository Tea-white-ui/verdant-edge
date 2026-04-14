package edge.verdant.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("machine_record")

public class MachineRecord {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO) // 指定主键生成策略
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
     * 警告内容
     */
    private String warning;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
