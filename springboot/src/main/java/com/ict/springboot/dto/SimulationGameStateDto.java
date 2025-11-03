package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.SimulationGameStateEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationGameStateDto {

    private Long id;
    private Long simulationId;
    private Integer inning;
    private String half;
    private Integer outs;
    private Long base1;
    private Long base2;
    private Long base3;
    private Integer homeScore;
    private Integer awayScore;
    private Integer homeBatterIdx;
    private Integer awayBatterIdx;
    private String gameStatus;
    private String winner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity -> DTO 변환
    public static SimulationGameStateDto toDto(SimulationGameStateEntity entity) {
        if (entity == null) return null;

        return SimulationGameStateDto.builder()
            .id(entity.getId())
            .simulationId(entity.getSimulation().getId())
            .inning(entity.getInning())
            .half(entity.getHalf())
            .outs(entity.getOuts())
            .base1(entity.getBase1())
            .base2(entity.getBase2())
            .base3(entity.getBase3())
            .homeScore(entity.getHomeScore())
            .awayScore(entity.getAwayScore())
            .homeBatterIdx(entity.getHomeBatterIdx())
            .awayBatterIdx(entity.getAwayBatterIdx())
            .gameStatus(entity.getGameStatus())
            .winner(entity.getWinner())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    // DTO -> Entity 변환 (새로 생성할 때)
    public SimulationGameStateEntity toEntity() {
        return SimulationGameStateEntity.builder()
            .id(this.id)
            .inning(this.inning)
            .half(this.half)
            .outs(this.outs)
            .base1(this.base1)
            .base2(this.base2)
            .base3(this.base3)
            .homeScore(this.homeScore)
            .awayScore(this.awayScore)
            .homeBatterIdx(this.homeBatterIdx)
            .awayBatterIdx(this.awayBatterIdx)
            .gameStatus(this.gameStatus)
            .winner(this.winner)
            .updatedAt(this.updatedAt)
            .build();
    }
}