package com.ict.springboot.repository;

import com.ict.springboot.entity.UserSimulRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface UserSimulRequestRepository extends JpaRepository<UserSimulRequestEntity, Long> {
    
    // 사용자별 요청 조회
    List<UserSimulRequestEntity> findByUserIdOrderByRequestAtDesc(Long userId);
    
    // 상태별 요청 조회
    List<UserSimulRequestEntity> findByStatusOrderByRequestAtDesc(String status);
    
    // 팀별 요청 조회 (홈팀 또는 어웨이팀)
    @Query("SELECT r FROM UserSimulRequestEntity r WHERE r.hometeam = :teamId OR r.awayteam = :teamId ORDER BY r.requestAt DESC")
    List<UserSimulRequestEntity> findByTeamParticipation(@Param("teamId") Long teamId);
    
    // 대기중인 요청 개수 (관리자 알림용)
    @Query("SELECT COUNT(r) FROM UserSimulRequestEntity r WHERE r.status = 'PENDING'")
    Long countByStatusPending();
    
    // 전체 요청 조회 (최신순)
    List<UserSimulRequestEntity> findAllByOrderByRequestAtDesc();

     @Query("SELECT r FROM UserSimulRequestEntity r " +
           "WHERE r.status = 'SCHEDULED' " +
           "AND r.scheduledAt IS NOT NULL " +
           "AND r.scheduledAt <= :currentTime " +
           "ORDER BY r.scheduledAt ASC")
    List<UserSimulRequestEntity> findScheduledRequestsToExecute(@Param("currentTime") LocalDateTime currentTime);
    
    // ⭐ 특정 상태와 시간 범위로 조회
    List<UserSimulRequestEntity> findByStatusAndScheduledAtBefore(String status, LocalDateTime scheduledAt);

    
}
