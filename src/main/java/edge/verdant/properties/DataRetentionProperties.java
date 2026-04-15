package edge.verdant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "verdant.data-retention")
@Data
public class DataRetentionProperties {
    private int machineRecordDays = 90;
    private int machineCameraDays = 90;
}