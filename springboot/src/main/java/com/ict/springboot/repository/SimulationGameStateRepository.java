package com.ict.springboot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.SimulationGameStateEntity;

@Repository
public interface SimulationGameStateRepository extends JpaRepository<SimulationGameStateEntity, Long> {

    // 시뮬레이션 ID로 게임 상태 조회
    @Query("SELECT sgs FROM SimulationGameStateEntity sgs WHERE sgs.simulation.id = :simulationId")
    Optional<SimulationGameStateEntity> findBySimulationId(@Param("simulationId") Long simulationId);

    // 시뮬레이션 ID로 게임 상태 존재 여부 확인
    @Query("SELECT COUNT(sgs) > 0 FROM SimulationGameStateEntity sgs WHERE sgs.simulation.id = :simulationId")
    boolean existsBySimulationId(@Param("simulationId") Long simulationId);
    
    // 게임 상태별 조회
    List<SimulationGameStateEntity> findByGameStatus(String gameStatus);
    
    // 오늘 날짜의 진행 중인 게임들만 조회
    @Query("SELECT sgs FROM SimulationGameStateEntity sgs WHERE sgs.gameStatus = :gameStatus AND sgs.simulation.showAt BETWEEN :startOfDay AND :endOfDay")
    List<SimulationGameStateEntity> findByGameStatusAndSimulationShowAtBetween(
        @Param("gameStatus") String gameStatus,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay);
    
    // 게임 상태와 업데이트 시간으로 조회 (향후 사용 가능)
    @Query("SELECT sgs FROM SimulationGameStateEntity sgs WHERE sgs.gameStatus = :gameStatus AND sgs.updatedAt < :beforeTime")
    List<SimulationGameStateEntity> findByGameStatusAndUpdatedAtBefore(
        @Param("gameStatus") String gameStatus, 
        @Param("beforeTime") LocalDateTime beforeTime);
}