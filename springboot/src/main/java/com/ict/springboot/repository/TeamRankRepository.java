package com.ict.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.TeamRankEntity;

@Repository
public interface TeamRankRepository extends JpaRepository<TeamRankEntity, TeamRankEntity.TeamRankId> {

    @Query("SELECT t FROM TeamRankEntity t WHERE t.year = :year ORDER BY CAST(t.rank AS int) ASC")
    List<TeamRankEntity> findByYearOrderByRankAsc(@Param("year") String year);
}

