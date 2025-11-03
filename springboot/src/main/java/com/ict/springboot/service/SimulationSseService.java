package com.ict.springboot.service;

import com.ict.springboot.dto.SimulationDto;
import com.ict.springboot.dto.UserSimulRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시뮬레이션 SSE 서비스
 * 시뮬레이션 요청/승인/거절 관련 실시간 이벤트 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationSseService {

    // SSE Emitter 저장소 (userId -> SseEmitter)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    private static final Long TIMEOUT = 60L * 60 * 1000; // 1시간

    // ===========================================
    // SSE 연결 관리
    // ===========================================
    
    /**
     * SSE 구독 (클라이언트가 연결)
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);
        
        log.info("✅ 시뮬레이션 SSE 연결: userId={}", userId);
        
        // 연결 완료 시 초기 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("시뮬레이션 SSE 연결 성공"));
        } catch (IOException e) {
            log.error("시뮬레이션 SSE 초기 메시지 전송 실패", e);
        }
        
        // 연결 종료 처리
        emitter.onCompletion(() -> {
            log.info("시뮬레이션 SSE 연결 종료: userId={}", userId);
            emitters.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.info("시뮬레이션 SSE 타임아웃: userId={}", userId);
            emitters.remove(userId);
        });
        
        emitter.onError(e -> {
            log.error("시뮬레이션 SSE 오류: userId={}", userId, e);
            emitters.remove(userId);
        });
        
        return emitter;
    }

    // ===========================================
    // 시뮬레이션 이벤트 발송
    // ===========================================
    
    /**
     * 새 시뮬레이션 요청 생성 시 모든 관리자에게 알림
     */
    public void sendNewRequestEvent(UserSimulRequestDto requestDto) {
        log.info("📤 새 시뮬레이션 요청 이벤트 발송: requestId={}", requestDto.getId());
        
        // 모든 연결된 사용자에게 이벤트 발송 (실제로는 관리자만 필터링해야 함)
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("newRequest")
                    .data(requestDto));
                
                log.info("✅ 새 요청 이벤트 전송 완료: userId={}, requestId={}", userId, requestDto.getId());
            } catch (IOException e) {
                log.error("❌ 새 요청 이벤트 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
        });
    }
    
    /**
     * 시뮬레이션 요청 상태 변경 시 (승인/거절) 관련 사용자에게 알림
     */
    public void sendRequestStatusChangeEvent(Long requestId, String status, String adminComment, Long requesterUserId) {
        log.info("📤 시뮬레이션 요청 상태 변경 이벤트 발송: requestId={}, status={}", requestId, status);
        
        // 요청자에게 상태 변경 알림
        if (requesterUserId != null) {
            SseEmitter requesterEmitter = emitters.get(requesterUserId);
            if (requesterEmitter != null) {
                try {
                    requesterEmitter.send(SseEmitter.event()
                        .name("requestStatusChanged")
                        .data(Map.of(
                            "requestId", requestId,
                            "status", status,
                            "adminComment", adminComment != null ? adminComment : ""
                        )));
                    
                    log.info("✅ 요청 상태 변경 이벤트 전송 완료: userId={}, requestId={}, status={}", 
                        requesterUserId, requestId, status);
                } catch (IOException e) {
                    log.error("❌ 요청 상태 변경 이벤트 전송 실패: userId={}", requesterUserId, e);
                    emitters.remove(requesterUserId);
                }
            }
        }
        
        // 모든 관리자에게도 알림 (관리자 목록 업데이트용)
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("requestStatusChanged")
                    .data(Map.of(
                        "requestId", requestId,
                        "status", status,
                        "adminComment", adminComment != null ? adminComment : ""
                    )));
                
                log.info("✅ 관리자에게 상태 변경 이벤트 전송: userId={}, requestId={}", userId, requestId);
            } catch (IOException e) {
                log.error("❌ 관리자 상태 변경 이벤트 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
        });
    }
    
    /**
     * 시뮬레이션 승인되어 새 시뮬레이션 생성 시 해당 사용자에게 알림
     */
    public void sendSimulationApprovedEvent(SimulationDto simulationDto) {
        log.info("📤 시뮬레이션 승인 이벤트 발송: simulationId={}", simulationDto.getId());
        
        // 사용자에게 새 시뮬레이션 알림
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("simulationApproved")
                    .data(simulationDto));
                
                log.info("✅ 시뮬레이션 승인 이벤트 전송 완료: userId={}, simulationId={}", 
                    userId, simulationDto.getId());
            } catch (IOException e) {
                log.error("❌ 시뮬레이션 승인 이벤트 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
        });
    }
    
    /**
     * 연결된 사용자 수 조회 (디버깅용)
     */
    public int getConnectedUserCount() {
        return emitters.size();
    }
    
    /**
     * 특정 사용자 연결 해제
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("🔌 시뮬레이션 SSE 연결 해제: userId={}", userId);
        }
    }
}
