package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.PlacesEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacesDto {
    private Long id;
    private UsersDto user;
    private TeamDto team;
    private String name;
    private String locAddress; //행정동 주소 (프론트에서 받음)
    private String address; //상세 주소(장소의 진짜 주소)
    private String image;
    private String category;
    private LocalDateTime createdAt;

    //DTO를 Entity로 변환하는 메서드
    public PlacesEntity toEntity() {
        UsersEntity usersEntity = user == null ? null : user.toEntity();
        TeamEntity teamEntity = team == null ? null : team.toEntity();
        return PlacesEntity.builder()
            .id(id)
            .user(usersEntity)
            .team(teamEntity)
            .name(name)
            .address(address)
            .image(image)
            .category(category)
            .createdAt(createdAt)
            .build();
    }

    //Entity를 DTO로 변환하는 메서드
    public static PlacesDto toDto(PlacesEntity placesEntity) {
        if(placesEntity==null) return null;
        return PlacesDto.builder()
        .id(placesEntity.getId())
        .user(UsersDto.toDto(placesEntity.getUser()))
        .team(TeamDto.toDto(placesEntity.getTeam()))
        .name(placesEntity.getName())
        .address(placesEntity.getAddress())
        .image(placesEntity.getImage())
        .category(placesEntity.getCategory())
        .createdAt(placesEntity.getCreatedAt())
        .build();
    }
}



