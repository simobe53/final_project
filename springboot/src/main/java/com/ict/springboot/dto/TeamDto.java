package com.ict.springboot.dto;

import com.ict.springboot.entity.TeamEntity;

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
public class TeamDto {
    private long id;
    private String idKey;
    private String name;
    private String location;
    private String stadium;

    public TeamEntity toEntity() {
        return TeamEntity.builder()
        .id(id)
        .idKey(idKey)
        .name(name)
        .location(location)
        .stadium(stadium)
        .build();
    }

    public static TeamDto toDto(TeamEntity entity) {
        if (entity == null) {
            return null;
        }
        return TeamDto.builder()
        .id(entity.getId())
        .idKey(entity.getIdKey())
        .name(entity.getName())
        .location(entity.getLocation())
        .stadium(entity.getStadium())
        .build();
    }
}
