package com.ict.springboot.repository;

import com.ict.springboot.entity.DiaryEntity;
import com.ict.springboot.entity.UsersEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaryRepository extends JpaRepository<DiaryEntity, Long> {
    Optional<DiaryEntity> findByDiaryDateAndUserId(LocalDate diaryDate, Long userId);
    @Query("SELECT d.diaryDate FROM DiaryEntity d WHERE d.user = :user")
    List<LocalDate> findAllDiaryDatesByUser(@Param("user") UsersEntity user);
    List<DiaryEntity> findByUserIdOrderByDiaryDateAsc(Long userId);
    @Query("SELECT COALESCE(COUNT(d),0) FROM DiaryEntity d WHERE d.user.id = :userId " +
        "AND d.user.team.name = :teamName")
    int countByUserAndTeam(@Param("userId") Long userId, @Param("teamName") String teamName);
    @Query("SELECT COALESCE(SUM(d.totalWins),0) FROM DiaryEntity d WHERE d.user.id = :userId " +
        "AND d.user.team.name = :teamName")
    int countByUserAndTeamWins(@Param("userId") Long userId, @Param("teamName") String teamName);

}