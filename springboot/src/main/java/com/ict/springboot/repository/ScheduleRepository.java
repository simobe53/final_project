package com.ict.springboot.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ict.springboot.dto.ScheduleDto;
import com.ict.springboot.entity.ScheduleEntity;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    // 날짜별 일정 조회
    @Query("""
        SELECT s FROM ScheduleEntity s
        WHERE s.homeTeam IS NOT NULL AND s.awayTeam IS NOT NULL
        AND s.gameDate = :date
        ORDER BY s.gameTime DESC
    """)
    List<ScheduleEntity> findByGameDateOrderByGameTimeDesc(LocalDate date);

    // 최신순 limit
    @Query("""
        SELECT s FROM ScheduleEntity s
        WHERE s.homeTeam IS NOT NULL AND s.awayTeam IS NOT NULL
        ORDER BY s.gameDate DESC, s.gameTime DESC
    """)
    List<ScheduleEntity> findLatest(Pageable pageable);

    // 최신순 하이라이트만 limit
    @Query("""
        SELECT s FROM ScheduleEntity s
        WHERE s.highlightUrl IS NOT NULL
        ORDER BY s.gameDate DESC, s.gameTime DESC
    """)
    List<ScheduleEntity> findLatestWithHighlightUrl(Pageable pageable);

    // 최신순 팀검색 하이라이트만 limit
    @Query("""
        SELECT s FROM ScheduleEntity s
        WHERE (s.homeTeam LIKE %:teamName% OR s.awayTeam LIKE %:teamName%)
        AND s.highlightUrl IS NOT NULL
        ORDER BY s.gameDate DESC, s.gameTime DESC
    """)
    List<ScheduleEntity> findLatestByTeamWithHighlightUrl(String teamName, Pageable pageable);

    // 최신순 팀검색 일정전부 limit
    @Query("""
        SELECT s FROM ScheduleEntity s
        WHERE (s.homeTeam LIKE %:teamName% OR s.awayTeam LIKE %:teamName%)
        ORDER BY s.gameDate DESC, s.gameTime DESC
    """)
    List<ScheduleEntity> findLatestByTeam(String teamName, Pageable pageable);

    @Query("""
        SELECT s FROM ScheduleEntity s
        WHERE (s.stadium IS NOT NULL AND s.stadium NOT LIKE '%미정%')
        AND s.gameDate >= SYSDATE
        ORDER BY s.gameDate ASC, s.gameTime ASC
    """)
    List<ScheduleEntity> findWithStadiumOrderByGameTimeDesc();

}
