package com.ict.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.AiUniformEntity;

@Repository
public interface AiUniformRepository extends JpaRepository<AiUniformEntity, Long> {
    
    // 사용자별 AI 유니폼 조회 (최신순)
    @Query("SELECT a FROM AiUniformEntity a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<AiUniformEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 팀별 AI 유니폼 조회 (최신순)
    @Query("SELECT a FROM AiUniformEntity a WHERE a.team.id = :teamId ORDER BY a.createdAt DESC")
    List<AiUniformEntity> findByTeamIdOrderByCreatedAtDesc(@Param("teamId") Long teamId);
    
    // 사용자와 팀별 AI 유니폼 조회 (최신순)
    @Query("SELECT a FROM AiUniformEntity a WHERE a.user.id = :userId AND a.team.id = :teamId ORDER BY a.createdAt DESC")
    List<AiUniformEntity> findByUserIdAndTeamIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("teamId") Long teamId);
    
    // 파일명으로 조회
    AiUniformEntity findByFilename(String filename);
    
}
