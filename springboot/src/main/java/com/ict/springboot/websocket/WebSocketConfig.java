package com.ict.springboot.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

	private final WebSocketServer wSocketServer;
	private final LocationWebSocketHandler locationWebSocketHandler;
	private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

	//<<<클라이언트 접속을 위한 엔드 포인트 설정>>>
	@Override
	public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
		// Simulation 채팅 전용 (/api/chat)
		registry.addHandler(wSocketServer, "/api/chat/**")
				.addInterceptors(jwtHandshakeInterceptor)
				.setAllowedOrigins("*");

		// Meet 위치공유 전용 (/api/location)
		registry.addHandler(locationWebSocketHandler, "/api/location/**")
				.addInterceptors(jwtHandshakeInterceptor)
				.setAllowedOrigins("*");
	}

	/**
	 * WebSocket 메시지 크기 제한 설정
	 * - 프로필 이미지 등 큰 데이터 전송을 위해 넉넉하게 설정
	 * - 기본값 8KB → 2MB로 증가
	 */
	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		// 텍스트 메시지 최대 크기: 2MB (프로필 이미지 base64 포함)
		container.setMaxTextMessageBufferSize(2 * 1024 * 1024);
		// 바이너리 메시지 최대 크기: 2MB
		container.setMaxBinaryMessageBufferSize(2 * 1024 * 1024);
		// 세션 idle 타임아웃: 10분 (600초)
		container.setMaxSessionIdleTimeout(10 * 60 * 1000L);
		return container;
	}
}
