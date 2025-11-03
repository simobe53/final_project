package com.ict.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CustomResourceConfig implements WebMvcConfigurer {

	//정적 리소스 설정을 추가할 수 있다
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		
		registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets");
		registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/");  // index.html 등 모든 정적 리소스 포함
		
	}

	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
	
	
}
