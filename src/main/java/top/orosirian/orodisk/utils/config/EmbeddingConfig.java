package top.orosirian.orodisk.utils.config;

import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.zhipuai.api-key}")
    private String zhipuAiApiKey;

    @Bean
    public ZhiPuAiEmbeddingModel embeddingModel() {
        return new ZhiPuAiEmbeddingModel(ZhiPuAiApi.builder().apiKey(zhipuAiApiKey).build());
    }

}
