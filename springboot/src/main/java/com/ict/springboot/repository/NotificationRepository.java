package com.ict.springboot.repository;

import com.ict.springboot.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    
    // 사용자별 알림 조회 (최신순)
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 읽지 않은 알림만 조회
    List<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    // 읽지 않은 알림 개수
    Long countByUserIdAndIsReadFalse(Long userId);
    
    // 특정 시뮬레이션 관련 알림
    List<NotificationEntity> findBySimulationIdOrderByCreatedAtDesc(Long simulationId);
}

