package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.MeetApplyEntity;
import com.ict.springboot.entity.MeetApplyEntity.ApprovalStatus;

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
public class MeetApplyDto {

    private long id;
    private String comments;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private UsersDto user;
    private MeetsDto meet;

    public MeetApplyEntity toEntity(){
        return MeetApplyEntity.builder()
                            .id(id)
                            .comments(comments)
                            .approvedAt(approvedAt)
                            .createdAt(createdAt)
                            .status(status)
                            .user(user == null ? null : user.toEntity())
                            .meet(meet.toEntity())
                            .build();
    }

    public static MeetApplyDto toDto(MeetApplyEntity mEntity){
        if(mEntity==null) return null;
        return MeetApplyDto.builder()
                            .id(mEntity.getId())
                            .comments(mEntity.getComments())
                            .approvedAt(mEntity.getApprovedAt())
                            .createdAt(mEntity.getCreatedAt())
                            .status(mEntity.getStatus())
                            .user(UsersDto.toDto(mEntity.getUser()))
                            .meet(MeetsDto.toDto(mEntity.getMeet()))
                            .build();
    }
}