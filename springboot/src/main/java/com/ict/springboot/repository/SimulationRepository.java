package com.ict.springboot.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.SimulationEntity;

@Repository
public interface SimulationRepository extends JpaRepository<SimulationEntity, Long> {
    
    // 작성자 ID로 시뮬레이션 조회
    List<SimulationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 홈팀으로 시뮬레이션 조회
    List<SimulationEntity> findByHometeamOrderByCreatedAtDesc(Long hometeam);
    
    // 어웨이팀으로 시뮬레이션 조회
    List<SimulationEntity> findByAwayteamOrderByCreatedAtDesc(Long awayteam);
    
    // 특정 팀이 참여한 모든 시뮬레이션 조회 (홈팀 또는 어웨이팀)
    @Query("SELECT s FROM SimulationEntity s WHERE s.hometeam = :teamId OR s.awayteam = :teamId ORDER BY s.createdAt DESC")
    List<SimulationEntity> findByTeamParticipation(@Param("teamId") Long teamId);
    
    // 매치 ID로 시뮬레이션 조회
    List<SimulationEntity> findByMatchIdOrderByCreatedAtDesc(String matchId);
    
    // 매치 ID 중복 조회
    boolean existsByMatchId(String matchId);
    
    // 전체 시뮬레이션 조회 (생성일 기준 내림차순)
    Page<SimulationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 특정 기간의 시뮬레이션 조회
    @Query("SELECT s FROM SimulationEntity s WHERE s.showAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<SimulationEntity> findByShowAtBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                               @Param("endDate") java.time.LocalDateTime endDate);
    
    // 홈팀과 어웨이팀으로 검색
    @Query("SELECT s FROM SimulationEntity s WHERE s.hometeam = :hometeam AND s.awayteam = :awayteam ORDER BY s.createdAt DESC")
    List<SimulationEntity> findByHometeamAndAwayteam(@Param("hometeam") Long hometeam, @Param("awayteam") Long awayteam);
    
}
