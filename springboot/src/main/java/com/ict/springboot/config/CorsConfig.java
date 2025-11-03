package com.ict.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    //CORS 오류 해결: 프론트엔드에서 백엔드 API 호출 시 CORS 오류 방지
    //쿠키 인증 지원: JWT 토큰을 쿠키로 전송할 수 있도록 허용
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                //.allowedOrigins("http://localhost:5173", "http://localhost:3000") // React 개발 서버
                .allowedOriginPatterns(
                    "http://localhost:*",           // 로컬 개발
                    "https://my-ball.site",         // 프로덕션
                    "https://*.my-ball.site",       // 서브도메인 포함
                    "http://my-ball.site"           // HTTP도 일단 허용
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 쿠키 허용 (중요!)
                .maxAge(3600);
    }

}
