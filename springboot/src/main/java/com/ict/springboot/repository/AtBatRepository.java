package com.ict.springboot.repository;

import com.ict.springboot.entity.AtBatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtBatRepository extends JpaRepository<AtBatEntity, Long> {
    
    /**
     * 특정 시뮬레이션의 모든 타석 데이터 조회
     */
    List<AtBatEntity> findBySimulation_Id(Long simulationId);

    /**
     * 특정 시뮬레이션의 모든 타석 데이터 조회 (이닝순 정렬)
     */
    List<AtBatEntity> findBySimulation_IdOrderByInningHalfAsc(Long simulationId);

    /**
     * 특정 시뮬레이션의 특정 이닝 타석 데이터 조회
     */
    @Query("SELECT a FROM AtBatEntity a WHERE a.simulation.id = :simulationId AND a.inningHalf LIKE :inning% ORDER BY a.inningHalf ASC")
    List<AtBatEntity> findBySimulationIdAndInning(@Param("simulationId") Long simulationId, @Param("inning") String inning);

    /**
     * 특정 시뮬레이션의 타석 수 조회
     */
    long countBySimulation_Id(Long simulationId);
    
    /**
     * 특정 시뮬레이션의 특정 이닝부터의 타석 데이터 삭제
     */
    @Query("DELETE FROM AtBatEntity a WHERE a.simulation.id = :simulationId AND a.inningHalf >= :fromInning")
    void deleteBySimulationIdAndFromInning(@Param("simulationId") Long simulationId, @Param("fromInning") String fromInning);
}
