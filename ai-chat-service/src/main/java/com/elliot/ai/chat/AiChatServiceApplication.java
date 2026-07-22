package com.elliot.ai.chat;

import com.elliot.ai.common.autoconfigure.CommonWebAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = CommonWebAutoConfiguration.class)
@MapperScan("com.elliot.ai.chat.mapper")
public class AiChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiChatServiceApplication.class, args);
    }

}
