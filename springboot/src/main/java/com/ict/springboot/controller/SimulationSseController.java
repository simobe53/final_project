package com.ict.springboot.controller;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.SimulationSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "시뮬레이션 SSE", description = "시뮬레이션 실시간 이벤트 API")
@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationSseController {

    private final SimulationSseService simulationSseService;

    /**
     * SSE 구독 (실시간 시뮬레이션 이벤트 받기)
     */
    @Operation(summary = "시뮬레이션 SSE 구독", description = "시뮬레이션 요청/승인/거절 관련 실시간 이벤트를 받기 위한 SSE 연결")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        
        return simulationSseService.subscribe(user.getId());
    }

    /**
     * 연결 상태 조회 (디버깅용)
     */
    @Operation(summary = "SSE 연결 상태 조회", description = "현재 연결된 사용자 수 조회 (디버깅용)")
    @GetMapping("/stream/status")
    public ResponseEntity<?> getConnectionStatus() {
        int connectedUsers = simulationSseService.getConnectedUserCount();
        return ResponseEntity.ok(Map.of(
            "connectedUsers", connectedUsers,
            "message", "현재 " + connectedUsers + "명이 연결되어 있습니다."
        ));
    }
}
