package edge.verdant.config;

import edge.verdant.properties.DashScopeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置类
 * 用于配置与 DashScope API 交互的 WebClient 实例
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final DashScopeProperties properties;
    
    /**
     * 创建用于访问 DashScope API 的 WebClient Bean
     *
     * @return 配置好的 WebClient 实例
     */
    @Bean
    public WebClient dashScopeWebClient() {
        return WebClient.builder()
                // 设置基础 URL
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                // 设置默认请求头：Content-Type
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // 设置默认请求头：Authorization，使用 API Key 进行认证
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                // 设置最大内存大小为 10MB
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}