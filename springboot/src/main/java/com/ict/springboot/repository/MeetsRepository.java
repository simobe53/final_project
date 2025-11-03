package com.ict.springboot.repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ict.springboot.entity.MeetsEntity;

public interface MeetsRepository extends JpaRepository<MeetsEntity,Long>{

    List<MeetsEntity> findAllByTeamId(long id);
    
    Page<MeetsEntity> findAllByTeamId(long id,Pageable pageable);

    List<MeetsEntity> findAllByIsActiveOrderByMeetAtAsc(boolean isActive);

    @Query(value = ("""
        SELECT m.* FROM meets m
        WHERE is_active = 1 and team_id = :team
        ORDER BY meet_at ASC 
        FETCH FIRST :limit ROWS ONLY
        """)
        ,nativeQuery = true)
    List<MeetsEntity> findClosingSoon(@Param("limit") int limit, @Param("team") long teamId);

    @Query(value = """
    SELECT 'feed' AS type, TRUNC(created_at) AS day, COUNT(*) AS count
    FROM feeds
    WHERE created_at >= TRUNC(SYSDATE - 7) AND created_at < TRUNC(SYSDATE)
    GROUP BY TRUNC(created_at)

    UNION ALL

    SELECT 'place' AS type, TRUNC(created_at) AS day, COUNT(*) AS count
    FROM places
    WHERE created_at >= TRUNC(SYSDATE - 7) AND created_at < TRUNC(SYSDATE)
    GROUP BY TRUNC(created_at)

    UNION ALL

    SELECT 'meet' AS type, TRUNC(created_at) AS day, COUNT(*) AS count
    FROM meets
    WHERE created_at >= TRUNC(SYSDATE - 7) AND created_at < TRUNC(SYSDATE)
    GROUP BY TRUNC(created_at)
    ORDER BY day
    """, nativeQuery = true)
    List<Object[]> getAllBbsCountBySeven();

    @Query(value = ("""
        SELECT m.* FROM meets m
        WHERE is_active = 1 and team_id = :team and created_at BETWEEN SYSDATE - 7 AND SYSDATE
        ORDER BY created_at DESC 
        FETCH FIRST :limit ROWS ONLY
        """)
        ,nativeQuery = true)
    List<MeetsEntity> findByTeamIdOrderByCommentsAndApplies(@Param("limit") int limit, @Param("team") long teamId);

    // 검색 기능을 위한 메서드
    Page<MeetsEntity> findByTitleContainingOrUser_NameContainingOrTeam_NameContaining(
        String title, String userName, String teamName, Pageable pageable);

}
