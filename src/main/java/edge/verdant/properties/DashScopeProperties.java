package edge.verdant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.dashscope")
@Data
public class DashScopeProperties {
    private String apiKey;
    private String model;
    private long timeout;
}
