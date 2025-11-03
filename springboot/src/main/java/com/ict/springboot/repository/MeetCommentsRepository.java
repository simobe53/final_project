package com.ict.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.springboot.entity.MeetCommentsEntity;

public interface MeetCommentsRepository extends JpaRepository<MeetCommentsEntity,Long> {

    void deleteAllByMeetId(long id);

    List<MeetCommentsEntity> findAllByMeetIdOrderById(long id);

}
