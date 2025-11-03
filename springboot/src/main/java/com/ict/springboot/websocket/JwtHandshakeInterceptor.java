package com.ict.springboot.websocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.ict.springboot.dto.UsersDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // JwtAuthenticationFilter에서 이미 검증하고 저장한 user 정보 가져오기
            UsersDto user = (UsersDto) httpRequest.getAttribute("user");

            if (user != null) {
                // HTTP 요청의 user 정보를 WebSocket 세션 attributes로 복사
                attributes.put("user", user);
                log.debug("WebSocket 인증 성공: {}", user.getAccount());
                return true;
            }
        }

        // 인증 실패 시 WebSocket 연결 거부
        log.warn("WebSocket 인증 실패");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 이후 처리 (필요시 구현)
    }
}