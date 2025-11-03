package com.ict.springboot.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.MeetCommentsDto;
import com.ict.springboot.entity.MeetCommentsEntity;
import com.ict.springboot.entity.MeetsEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.MeetCommentsRepository;
import com.ict.springboot.repository.MeetsRepository;
import com.ict.springboot.repository.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class MeetCommentsService {
    
    private final MeetCommentsRepository meetCommentRepo;
    private final UsersRepository userRepo;
    private final MeetsRepository meetRepo;

    //<<댓글 생성>>
    public MeetCommentsDto createComment(MeetCommentsDto dto) {
        UsersEntity user = userRepo.findById(dto.getUser().getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        MeetsEntity meet = meetRepo.findById(dto.getMeet().getId()).orElseThrow(() -> new EntityNotFoundException("Meet not found"));
        MeetCommentsEntity commentsEntity = dto.toEntity();
        commentsEntity.setUser(user);
        commentsEntity.setMeet(meet);
        return MeetCommentsDto.toDto(meetCommentRepo.save(commentsEntity));
    }
    //<<댓글 조회>>
    public List<MeetCommentsDto> getComment(long id) {
        List<MeetCommentsEntity> meetCommentsEntities = meetCommentRepo.findAllByMeetIdOrderById(id);
        return meetCommentsEntities.stream().map(entity->MeetCommentsDto.toDto(entity)).collect(Collectors.toList());
    }
    //<<댓글 삭제>>
    @Transactional
    public void deleteComment(long id) {
        meetCommentRepo.deleteById(id);
    }
    public List<MeetCommentsDto> getCommentByAdmin(long id) {
        List<MeetCommentsEntity> meetCommentsEntities = meetCommentRepo.findAllByMeetIdOrderById(id);
        return meetCommentsEntities.stream().map(entity->MeetCommentsDto.toDto(entity)).collect(Collectors.toList());
    }
 
}
