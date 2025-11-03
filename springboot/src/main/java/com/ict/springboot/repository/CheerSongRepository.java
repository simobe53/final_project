package com.ict.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.CheerSongEntity;

@Repository
public interface CheerSongRepository extends JpaRepository<CheerSongEntity, Long> {

    // 공유된 응원곡 목록 조회 (생성일 내림차순)
    List<CheerSongEntity> findByIsSharedTrueOrderByCreatedAtDesc();

    // 특정 사용자의 응원곡 목록 조회
    List<CheerSongEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 팀의 공유된 응원곡 목록 조회
    List<CheerSongEntity> findByTeamIdAndIsSharedTrueOrderByCreatedAtDesc(Long teamId);
}
