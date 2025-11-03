package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.MeetsEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetsDto {
    private long id;
    private String title;
    private String content;
    private int goal;
    private LocalDateTime meetAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private boolean isActive = true;

    private UsersDto user;
    private TeamDto team;

    //<<dto 에서만 존재 >>
    //로그인한 유저의 지원 여부
    private boolean isApply;
    //로그인한 유저의 작성 여부
    private boolean isWriter; 
    //승인받은 사람 수
    private int approved;
    //전체 지원자 수
    private int totalApply;
    

    public MeetsEntity toEntity(){
        return MeetsEntity.builder()
                        .id(id)
                        .title(title)
                        .content(content)
                        .goal(goal)
                        .meetAt(meetAt)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .isActive(isActive)
                        .user(user == null ? null : user.toEntity())
                        .team(team == null ? null : team.toEntity()).build();        
    }

    public static MeetsDto toDto(MeetsEntity mEntity){
        if(mEntity==null) return null;
        
        return MeetsDto.builder()
                        .id(mEntity.getId())
                        .title(mEntity.getTitle())
                        .content(mEntity.getContent())
                        .goal(mEntity.getGoal())
                        .meetAt(mEntity.getMeetAt())
                        .createdAt(mEntity.getCreatedAt())
                        .updatedAt(mEntity.getUpdatedAt())
                        .isActive(mEntity.isActive())
                        .user(UsersDto.toDto(mEntity.getUser()))
                        .team(TeamDto.toDto(mEntity.getTeam())).build();     
    }


}
