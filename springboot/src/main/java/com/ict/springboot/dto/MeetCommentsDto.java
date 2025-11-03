package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.MeetCommentsEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetCommentsDto {
    private long id;
    private String content;
    private LocalDateTime createdAt;

    private UsersDto user;
    private MeetsDto meet;

    public MeetCommentsEntity toEntity(){
        return MeetCommentsEntity.builder()
                            .id(id)
                            .content(content)
                            .createdAt(createdAt)
                            .user(user == null ? null : user.toEntity()) //UsersEntity user;
                            .meet(meet == null ? null : meet.toEntity())
                            .build();
    }

    public static MeetCommentsDto toDto(MeetCommentsEntity mEntity){
        if(mEntity==null) return null;
        return MeetCommentsDto.builder()
                            .id(mEntity.getId())
                            .content(mEntity.getContent())
                            .createdAt(mEntity.getCreatedAt())
                            .user(UsersDto.toDto(mEntity.getUser()))
                            .meet(MeetsDto.toDto(mEntity.getMeet()))
                            .build();
    }

}

