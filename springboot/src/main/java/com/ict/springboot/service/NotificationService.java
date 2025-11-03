package com.ict.springboot.service;

import com.ict.springboot.dto.NotificationDto;
import com.ict.springboot.entity.NotificationEntity;
import com.ict.springboot.entity.NotificationSentLogEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.NotificationRepository;
import com.ict.springboot.repository.NotificationSentLogRepository;
import com.ict.springboot.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 * SSE를 통한 실시간 알림 + DB 영구 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final NotificationSentLogRepository sentLogRepo;
    private final UsersRepository usersRepo;
    
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
        
        log.info("✅ SSE 연결: userId={}", userId);
        
        // 연결 완료 시 초기 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패", e);
        }
        
        // 연결 종료 처리
        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료: userId={}", userId);
            emitters.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: userId={}", userId);
            emitters.remove(userId);
        });
        
        emitter.onError(e -> {
            log.error("SSE 오류: userId={}", userId, e);
            emitters.remove(userId);
        });
        
        return emitter;
    }

    // ===========================================
    // 알림 발송 (SSE + DB)
    // ===========================================
    
    /**
     * 특정 사용자에게 알림 전송
     */
    @Transactional
    public void sendToUser(Long userId, NotificationDto notificationDto) {
        try {
            // 1. DB에 저장
            UsersEntity user = usersRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            NotificationEntity entity = NotificationEntity.builder()
                .user(user)
                .simulationId(notificationDto.getSimulationId())
                .notificationType(notificationDto.getNotificationType())
                .title(notificationDto.getTitle())
                .message(notificationDto.getMessage())
                .link(notificationDto.getLink())
                .isRead(false)
                .isUrgent(notificationDto.getIsUrgent() != null ? notificationDto.getIsUrgent() : false)
                .homeTeamId(notificationDto.getHomeTeamId())
                .awayTeamId(notificationDto.getAwayTeamId())
                .build();
            
            entity = notificationRepo.save(entity);
            notificationDto.setId(entity.getId());
            notificationDto.setUserId(userId);
            notificationDto.setCreatedAt(entity.getCreatedAt());
            
            log.info("📥 알림 DB 저장: userId={}, type={}", userId, notificationDto.getNotificationType());
            
            // 2. SSE로 실시간 전송
            SseEmitter emitter = emitters.get(userId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificationDto));
                    
                    log.info("📤 알림 SSE 전송 완료: userId={}, type={}", userId, notificationDto.getNotificationType());
                } catch (IOException e) {
                    log.error("❌ SSE 전송 실패: userId={}", userId, e);
                    emitters.remove(userId);
                }
            } else {
                log.warn("⚠️ SSE 연결 없음 (오프라인): userId={}", userId);
            }
            
        } catch (Exception e) {
            log.error("❌ 알림 전송 실패: userId={}", userId, e);
        }
    }

    /**
     * 여러 사용자에게 알림 전송
     */
    public void sendToUsers(List<Long> userIds, NotificationDto notificationDto) {
        userIds.forEach(userId -> sendToUser(userId, notificationDto));
    }

    // ===========================================
    // 알림 조회/관리
    // ===========================================
    
    /**
     * 사용자의 모든 알림 조회
     */
    public List<NotificationDto> getAllNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationDto::toDto)
            .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림만 조회
     */
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepo.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationDto::toDto)
            .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림 개수
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        NotificationEntity notification = notificationRepo.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        
        notification.setIsRead(true);
        notificationRepo.save(notification);
        
        log.info("✅ 알림 읽음 처리: id={}", notificationId);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<NotificationEntity> notifications = 
            notificationRepo.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepo.saveAll(notifications);
        
        log.info("✅ 모든 알림 읽음 처리: userId={}, count={}", userId, notifications.size());
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepo.deleteById(notificationId);
        log.info("🗑️ 알림 삭제: id={}", notificationId);
    }

    // ===========================================
    // 중복 발송 방지
    // ===========================================
    
    /**
     * 이미 발송된 알림인지 체크
     */
    public boolean hasBeenSent(Long simulationId, String notificationType) {
        return sentLogRepo.existsBySimulationIdAndNotificationType(simulationId, notificationType);
    }

    /**
     * 발송 로그 기록
     */
    @Transactional
    public void markAsSent(Long simulationId, String notificationType) {
        if (!hasBeenSent(simulationId, notificationType)) {
            NotificationSentLogEntity logEntity = NotificationSentLogEntity.builder()
                .simulationId(simulationId)
                .notificationType(notificationType)
                .build();
            
            sentLogRepo.save(logEntity);
            log.info("📝 발송 로그 기록: simulationId={}, type={}", simulationId, notificationType);
        }
    }

    // ===========================================
    // 구체적인 알림 발송 메서드
    // ===========================================

    /**
     * 1. 신규 요청 알림 (사용자 → 관리자)
     */
    public void notifyNewSimulationRequest(Long requestId, String userName, LocalDateTime requestDate) {
        List<UsersEntity> admins = usersRepo.searchByParams(null, null, null, null, "ADMIN");
        
        // 요청 날짜를 YYYY-MM-DD 형식으로 변환
        String dateParam = requestDate != null ? requestDate.toLocalDate().toString() : LocalDateTime.now().toLocalDate().toString();
        
        NotificationDto notification = NotificationDto.builder()
            .notificationType("REQUEST_CREATED")
            .title("📢 새로운 시뮬레이션 요청")
            .message(userName + "님이 시뮬레이션을 요청했습니다.")
            .link("/simulate?date=" + dateParam)
            .isUrgent(true)
            .build();
        
        List<Long> adminIds = admins.stream()
            .map(UsersEntity::getId)
            .collect(Collectors.toList());
        
        sendToUsers(adminIds, notification);
        
        log.info("🔔 신규 요청 알림 발송: requestId={}, admins={}", requestId, adminIds.size());
    }

    /**
     * 2. 요청 승인 알림 (관리자 → 사용자) - 팀 정보 포함
     */
    public void notifyRequestApproved(Long userId, Long simulationId, LocalDateTime scheduledAt, Long homeTeamId, Long awayTeamId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");
        
        String message = scheduledAt != null
            ? "시뮬레이션이 승인되었습니다. " + scheduledAt.format(formatter) + "에 시작됩니다."
            : "시뮬레이션이 승인되었습니다.";
        
        NotificationDto notification = NotificationDto.builder()
            .simulationId(simulationId)
            .notificationType("REQUEST_APPROVED")
            .title("✅ 시뮬레이션 승인")
            .message(message)
            .link("/simulate/" + simulationId)
            .isUrgent(false)
            .homeTeamId(homeTeamId)
            .awayTeamId(awayTeamId)
            .build();
        
        sendToUser(userId, notification);
        
        log.info("🔔 승인 알림 발송: userId={}, simulationId={}, homeTeam={}, awayTeam={}", userId, simulationId, homeTeamId, awayTeamId);
    }

    /**
     * 3. 요청 거절 알림 (관리자 → 사용자) - 팀 정보 포함
     */
    public void notifyRequestRejected(Long userId, String adminComment, Long homeTeamId, Long awayTeamId) {
        NotificationDto notification = NotificationDto.builder()
            .notificationType("REQUEST_REJECTED")
            .title("❌ 시뮬레이션 거절")
            .message("시뮬레이션 요청이 거절되었습니다. 사유: " + adminComment)
            .link("/simulate")
            .isUrgent(false)
            .homeTeamId(homeTeamId)
            .awayTeamId(awayTeamId)
            .build();
        
        sendToUser(userId, notification);
        
        log.info("🔔 거절 알림 발송: userId={}, homeTeam={}, awayTeam={}", userId, homeTeamId, awayTeamId);
    }

    /**
     * 4. 시작 10분 전 알림 - 팀 정보 포함
     */
    public void notifySimulationReminder10(Long userId, Long simulationId, Long homeTeamId, Long awayTeamId) {
        if (hasBeenSent(simulationId, "SIMULATION_REMINDER_10")) {
            return;
        }
        
        NotificationDto notification = NotificationDto.builder()
            .simulationId(simulationId)
            .notificationType("SIMULATION_REMINDER_10")
            .title("⏰ 시뮬레이션 시작 임박")
            .message("10분 후 시뮬레이션이 시작됩니다!")
            .link("/simulate/" + simulationId)
            .isUrgent(true)
            .homeTeamId(homeTeamId)
            .awayTeamId(awayTeamId)
            .build();
        
        sendToUser(userId, notification);
        markAsSent(simulationId, "SIMULATION_REMINDER_10");
        
        log.info("🔔 10분 전 알림 발송: userId={}, simulationId={}, homeTeam={}, awayTeam={}", userId, simulationId, homeTeamId, awayTeamId);
    }

    /**
     * 5. 시작 5분 전 알림 - 팀 정보 포함
     */
    public void notifySimulationReminder5(Long userId, Long simulationId, Long homeTeamId, Long awayTeamId) {
        if (hasBeenSent(simulationId, "SIMULATION_REMINDER_5")) {
            return;
        }
        
        NotificationDto notification = NotificationDto.builder()
            .simulationId(simulationId)
            .notificationType("SIMULATION_REMINDER_5")
            .title("⏰ 곧 시작됩니다!")
            .message("5분 후 시뮬레이션이 시작됩니다!")
            .link("/simulate/" + simulationId)
            .isUrgent(true)
            .homeTeamId(homeTeamId)
            .awayTeamId(awayTeamId)
            .build();
        
        sendToUser(userId, notification);
        markAsSent(simulationId, "SIMULATION_REMINDER_5");
        
        log.info("🔔 5분 전 알림 발송: userId={}, simulationId={}, homeTeam={}, awayTeam={}", userId, simulationId, homeTeamId, awayTeamId);
    }

    /**
     * 6. 시뮬레이션 시작 알림 - 팀 정보 포함
     */
    public void notifySimulationStarted(Long userId, Long simulationId, Long homeTeamId, Long awayTeamId) {
        if (hasBeenSent(simulationId, "SIMULATION_STARTED")) {
            return;
        }
        
        NotificationDto notification = NotificationDto.builder()
            .simulationId(simulationId)
            .notificationType("SIMULATION_STARTED")
            .title("🎮 경기 시작!")
            .message("시뮬레이션 경기가 시작되었습니다! 지금 바로 관전하세요!")
            .link("/simulate/" + simulationId)
            .isUrgent(true)
            .homeTeamId(homeTeamId)
            .awayTeamId(awayTeamId)
            .build();
        
        sendToUser(userId, notification);
        markAsSent(simulationId, "SIMULATION_STARTED");
        
        log.info("🔔 시작 알림 발송: userId={}, simulationId={}, homeTeam={}, awayTeam={}", userId, simulationId, homeTeamId, awayTeamId);
    }

    /**
     * 7. 게임 종료 알림 - 팀 정보 포함
     */
    public void notifyGameEnded(Long userId, Long simulationId, String winner, int homeScore, int awayScore, Long homeTeamId, Long awayTeamId) {
        String winnerText = "HOME".equals(winner) ? "홈팀 승리" : "원정팀 승리";
        
        NotificationDto notification = NotificationDto.builder()
            .simulationId(simulationId)
            .notificationType("GAME_ENDED")
            .title("🏁 게임 종료")
            .message("게임이 종료되었습니다. " + winnerText + "! (" + awayScore + " - " + homeScore + ")")
            .link("/simulate/" + simulationId)
            .isUrgent(true)
            .homeTeamId(homeTeamId)
            .awayTeamId(awayTeamId)
            .build();
        
        sendToUser(userId, notification);
        
        log.info("🔔 종료 알림 발송: userId={}, simulationId={}, winner={}, homeTeam={}, awayTeam={}", userId, simulationId, winner, homeTeamId, awayTeamId);
    }
}

