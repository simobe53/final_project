package com.ict.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ict.springboot.entity.MeetApplyEntity;
import com.ict.springboot.entity.MeetApplyEntity.ApprovalStatus;

public interface MeetApplyRepository extends JpaRepository<MeetApplyEntity,Long>{

    List<MeetApplyEntity> findAllByUserId(long userId);

    boolean existsByUserIdAndMeetId(long userId, long meetId);
    
    List<MeetApplyEntity> findAllByMeetIdOrderByIdAsc(long meetId);

    int countByMeetIdAndStatus(long meetId, ApprovalStatus approved);

    void deleteAllByMeetId(long id);

    Optional<MeetApplyEntity> findByMeetIdAndUserId(long meetId, long userId);

    List<MeetApplyEntity> findAllByMeetId(long id);

    @Query("""
    SELECT COUNT(m) 
    FROM MeetApplyEntity m 
    WHERE m.meet.id = :meetId 
      AND (m.status = :pending OR m.status = :approved)
    """)
    int countByMeetIdAndStatus(@Param("meetId") long meetId,@Param("approved") ApprovalStatus approved,@Param("pending") ApprovalStatus pending);

    int countByMeetId(long id);

} 
