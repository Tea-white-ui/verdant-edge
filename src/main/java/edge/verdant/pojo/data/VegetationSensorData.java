package edge.verdant.pojo.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VegetationSensorData {
    /**
     * 土壤湿度 (%)
     */
    private BigDecimal soilMoisture;

    /**
     * 土壤温度 (°C)
     */
    //private BigDecimal soilTemp;

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
    //private BigDecimal co2;

    /**
     * 光照强度 (Lux)
     */
    private BigDecimal lightIntensity;

    /**
     * 成长阶段
     */
    //private Integer growthStage;

    /**
     * 警告内容
     */
    //private String warning;

    /**
     * 创建时间
     */
    //private LocalDateTime createTime;
    /**
     * 植物名称
     */
    private String plantName;
}