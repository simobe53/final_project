package com.ict.springboot.repository;

import com.ict.springboot.entity.NotificationSentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationSentLogRepository extends JpaRepository<NotificationSentLogEntity, Long> {
    
    // 중복 발송 체크
    boolean existsBySimulationIdAndNotificationType(Long simulationId, String notificationType);
}

