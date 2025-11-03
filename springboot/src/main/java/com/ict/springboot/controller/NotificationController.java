package com.ict.springboot.controller;

import com.ict.springboot.dto.NotificationDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "알림", description = "실시간 알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * SSE 구독 (실시간 알림 받기)
     */
    @Operation(summary = "SSE 구독", description = "실시간 알림을 받기 위한 SSE 연결")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        
        return notificationService.subscribe(user.getId());
    }

    /**
     * 모든 알림 조회
     */
    @Operation(summary = "모든 알림 조회")
    @GetMapping("")
    public ResponseEntity<?> getAllNotifications(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        
        List<NotificationDto> notifications = notificationService.getAllNotifications(user.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 조회
     */
    @Operation(summary = "읽지 않은 알림 조회")
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(user.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수
     */
    @Operation(summary = "읽지 않은 알림 개수")
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        
        Long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 알림 읽음 처리
     */
    @Operation(summary = "알림 읽음 처리")
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Operation(summary = "모든 알림 읽음 처리")
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 알림 삭제
     */
    @Operation(summary = "알림 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 알림 전송 (외부 API용)
     */
    @Operation(summary = "알림 전송", description = "FastAPI 등 외부 서비스에서 사용자에게 알림 전송")
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationDto notificationDto) {
        try {
            notificationService.sendToUser(notificationDto.getUserId(), notificationDto);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

