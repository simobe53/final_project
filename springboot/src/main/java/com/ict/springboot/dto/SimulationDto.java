package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.SimulationEntity;
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
public class SimulationDto {
    private Long id;
    private Long hometeam;
    private Long awayteam;
    private String homeLineup;
    private String awayLineup;
    private String matchId;
    private UsersDto user;
    private Boolean isFinished;
    private LocalDateTime showAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO를 Entity로 변환하는 메서드
    public SimulationEntity toEntity() {
        UsersEntity usersEntity = user == null ? null : user.toEntity();
        return SimulationEntity.builder()
            .id(id)
            .hometeam(hometeam)
            .awayteam(awayteam)
            .homeLineup(homeLineup)
            .awayLineup(awayLineup)
            .matchId(matchId)
            .user(usersEntity)
            .showAt(showAt)
            .isFinished(isFinished)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    // Entity를 DTO로 변환하는 메서드
    public static SimulationDto toDto(SimulationEntity simulationEntity) {
        if (simulationEntity == null) return null;
        return SimulationDto.builder()
            .id(simulationEntity.getId())
            .hometeam(simulationEntity.getHometeam())
            .awayteam(simulationEntity.getAwayteam())
            .homeLineup(simulationEntity.getHomeLineup())
            .awayLineup(simulationEntity.getAwayLineup())
            .matchId(simulationEntity.getMatchId())
            .user(UsersDto.toDto(simulationEntity.getUser()))
            .showAt(simulationEntity.getShowAt())
            .isFinished(simulationEntity.getIsFinished())
            .createdAt(simulationEntity.getCreatedAt())
            .updatedAt(simulationEntity.getUpdatedAt())
            .build();
    }
}
