package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;

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
public class UsersDto {
    private long id;
    private String account;
    private String password;
    private String name;
    private String email;
    private String gender;
    private String method;

    @Builder.Default
    private String role = "USER";

    private String profileImage;
    private LocalDateTime createdAt;
    private TeamDto team;

    @Builder.Default
    private Long point = 0L; // 유저 충전 금액

    private int notiCount; // 로그인시 뿌려줄 알림 갯수. setter 만 필요하므로 아래 toEntity toDto에는 넣어줄 필요없음

    public UsersEntity toEntity(){
        TeamEntity teamEtt = team == null ? null : team.toEntity();
        return UsersEntity.builder()
                        .id(id)
                        .account(account)
                        .password(password)
                        .name(name)
                        .email(email)
                        .gender(gender)
                        .role(role)
                        .method(method)
                        .profileImage(profileImage)
                        .createdAt(createdAt)
                        .team(teamEtt)
                        .point(point)
                        .build();
    }

    //회원가입할 때 쓸 dto
    public static UsersDto toDto(UsersEntity uEntity){
        if(uEntity==null) return null;
        return UsersDto.builder()
                    .id(uEntity.getId())
                    .account(uEntity.getAccount())
                    .password(uEntity.getPassword())
                    .name(uEntity.getName())
                    .email(uEntity.getEmail())
                    .gender(uEntity.getGender())
                    .role(uEntity.getRole())
                    .method(uEntity.getMethod())
                    .profileImage(uEntity.getProfileImage())
                    .createdAt(uEntity.getCreatedAt())
                    .team(TeamDto.toDto(uEntity.getTeam()))
                    .point(uEntity.getPoint())
                    .build();
    }

    //세션에 넣을 값은 비밀번호를 제외하고 빌드
    public static UsersDto sessionDto(UsersEntity uEntity){
        if(uEntity==null) return null;
        return UsersDto.builder()
                    .id(uEntity.getId())
                    .account(uEntity.getAccount())
                    .name(uEntity.getName())
                    .gender(uEntity.getGender())
                    .role(uEntity.getRole())
                    .method(uEntity.getMethod())
                    .profileImage(uEntity.getProfileImage())
                    .createdAt(uEntity.getCreatedAt())
                    .team(TeamDto.toDto(uEntity.getTeam()))
                    .point(uEntity.getPoint())
                    .build();
    }
}
