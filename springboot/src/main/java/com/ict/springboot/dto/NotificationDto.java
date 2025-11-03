package com.ict.springboot.dto;

import com.ict.springboot.entity.NotificationEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    
    private Long id;
    private Long userId;
    private Long simulationId;
    private String notificationType;
    private String title;
    private String message;
    private String link;
    private Boolean isRead;
    private Boolean isUrgent;
    private LocalDateTime createdAt;
    
    // 팀 정보 추가 (시뮬레이션 관련 알림용)
    private Long homeTeamId;
    private Long awayTeamId;
    
    // Entity -> DTO
    public static NotificationDto toDto(NotificationEntity entity) {
        if (entity == null) return null;
        
        return NotificationDto.builder()
            .id(entity.getId())
            .userId(entity.getUser().getId())
            .simulationId(entity.getSimulationId())
            .notificationType(entity.getNotificationType())
            .title(entity.getTitle())
            .message(entity.getMessage())
            .link(entity.getLink())
            .isRead(entity.getIsRead())
            .isUrgent(entity.getIsUrgent())
            .createdAt(entity.getCreatedAt())
            .homeTeamId(entity.getHomeTeamId())
            .awayTeamId(entity.getAwayTeamId())
            .build();
    }
}

