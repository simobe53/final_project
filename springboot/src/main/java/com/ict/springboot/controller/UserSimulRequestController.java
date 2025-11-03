package com.ict.springboot.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.SimulationDto;
import com.ict.springboot.dto.UserSimulRequestDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.UserSimulRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "사용자 시뮬레이션 요청", description = "사용자가 시뮬레이션을 요청하고 관리자가 승인/거절하는 API")
@RestController
@RequestMapping("/api/user-simul-requests")
@RequiredArgsConstructor
public class UserSimulRequestController {

    private final UserSimulRequestService userSimulRequestService;

    @Operation(summary = "시뮬레이션 요청 생성", description = "일반 사용자가 시뮬레이션 요청 생성")
    @PostMapping("")
    public ResponseEntity<?> createRequest(
            @RequestBody UserSimulRequestDto dto,
            HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            UserSimulRequestDto result = userSimulRequestService.create(dto, loginUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "요청 목록 조회", description = "관리자: 모든 요청, 일반 사용자: 본인 요청만 조회 (status 파라미터로 필터링 가능)")
    @GetMapping("")
    public ResponseEntity<?> getAllRequests(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            List<UserSimulRequestDto> requests = userSimulRequestService.getAll(loginUser, params);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "요청 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(
            @PathVariable Long id,
            HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            UserSimulRequestDto requestDto = userSimulRequestService.getById(id, loginUser);
            if (requestDto != null) {
                return ResponseEntity.ok(requestDto);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "요청 승인", description = "관리자가 사용자 요청 승인")
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            String adminComment = (String) requestBody.getOrDefault("adminComment", "승인됨");
            String scheduledAtStr = (String) requestBody.get("scheduledAt");
            LocalDateTime scheduledAt = null;
            if (scheduledAtStr != null && !scheduledAtStr.isEmpty()) {
                scheduledAt = LocalDateTime.parse(scheduledAtStr);
            }

            SimulationDto result = userSimulRequestService.approveRequest(id, adminComment, scheduledAt, loginUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "요청 거절", description = "관리자가 사용자 요청 거절")
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            String adminComment = requestBody.getOrDefault("adminComment", "거절됨");
            UserSimulRequestDto result = userSimulRequestService.rejectRequest(id, adminComment, loginUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "대기 중인 요청 개수", description = "관리자 알림용 대기 중인 요청 개수 조회")
    @GetMapping("/pending-count")
    public ResponseEntity<?> getPendingRequestCount(HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            Long count = userSimulRequestService.countPendingRequests(loginUser);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
