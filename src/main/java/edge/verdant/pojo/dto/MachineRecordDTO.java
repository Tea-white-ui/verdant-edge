package edge.verdant.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MachineRecordDTO {
    private int number;// 设备编号
    private BigDecimal soilMoisture;// 土壤湿度
    private BigDecimal airTemp;// 空气温度
    private BigDecimal airHumidity;// 空气湿度
    private BigDecimal lightIntensity;// 光照强度
    private String createTime;// 创建时间
}
