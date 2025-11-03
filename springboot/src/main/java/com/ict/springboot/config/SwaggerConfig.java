package com.ict.springboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    name = "springdoc.swagger-ui.enabled",
    havingValue = "true",
    matchIfMissing = false  // 환경 변수 없으면 비활성화 (보안 기본값)
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("My Ball API")
                .description("Spring Boot REST API 문서화")
                .version("v1.0")
                // .contact(new Contact()
                //     .name("ict")
                //     .email("dev@example.com"))
                    );
    }
}
