package com.ict.springboot.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.NewsEntity;

@Repository
public interface NewsRepository extends JpaRepository<NewsEntity, Long> {
    
    // 팀별 뉴스 조회 (최신순, 개수 제한) - ID 오름차순
    List<NewsEntity> findByTeamIdOrderByIdAsc(String teamId, Pageable pageable);
    
    // 전체 뉴스 조회 (최신순, 개수 제한) - ID 오름차순
    List<NewsEntity> findAllByOrderByIdAsc(Pageable pageable);
}

