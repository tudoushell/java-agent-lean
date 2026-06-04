package com.elliot.ai.chat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiChatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Chat Service API")
                        .description("基于 Spring AI 的多场景对话服务接口文档")
                        .version("v0.0.1"));
    }
}
