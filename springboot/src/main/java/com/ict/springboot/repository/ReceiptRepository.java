package com.ict.springboot.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.ict.springboot.entity.ReceiptEntity;

@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptEntity, Long> {
    
    // 사용자 ID로 영수증 조회 (최신순)
    @Query("SELECT r FROM ReceiptEntity r WHERE r.userId = :userId ORDER BY r.created_at DESC")
    List<ReceiptEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    @Query("SELECT r FROM ReceiptEntity r WHERE r.userId = :userId AND r.created_at > :dateTime")
    List<ReceiptEntity> findRecentByUserId(@Param("userId") Long userId, @Param("dateTime") LocalDateTime dateTime);


}