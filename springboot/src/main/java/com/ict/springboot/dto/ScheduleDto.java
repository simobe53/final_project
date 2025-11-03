package com.ict.springboot.dto;

import java.time.LocalDate;

import com.ict.springboot.entity.ScheduleEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDto {
    private Long id;
    private LocalDate gameDate;
    private String gameTime;
    private String stadium;
    private String remarks;
    private String boardse;
    private String awayTeam;
    private Integer awayTeamScore;
    private String homeTeam;
    private Integer homeTeamScore;
    private String victoryTeam;
    private Integer year;
    private Integer month;
    private String recordType;
    private String highlightUrl;
    private String highlightThumb;
    private String highlightSummary;
    private String nowInning; // toDto toEntity에 넣지 말것(진행중인 경기를 프론트로 보내기 위해 위해 존재하는 필드)

    public static ScheduleDto toDto(ScheduleEntity entity) {
        return ScheduleDto.builder()
        .id(entity.getId())
        .gameDate(entity.getGameDate())
        .gameTime(entity.getGameTime())
        .stadium(entity.getStadium())
        .remarks(entity.getRemarks())
        .boardse(entity.getBoardse())
        .awayTeam(entity.getAwayTeam())
        .awayTeamScore(entity.getAwayTeamScore())
        .homeTeam(entity.getHomeTeam())
        .homeTeamScore(entity.getHomeTeamScore())
        .victoryTeam(entity.getVictoryTeam())
        .year(entity.getYear())
        .month(entity.getMonth())
        .recordType(entity.getRecordType())
        .highlightUrl(entity.getHighlightUrl())
        .highlightThumb(entity.getHighlightThumb())
        .highlightSummary(entity.getHighlightSummary())
        .build();
    }

    public static ScheduleEntity toEntity(ScheduleDto dto) {
        return ScheduleEntity.builder()
        .id(dto.getId())
        .gameDate(dto.getGameDate())
        .gameTime(dto.getGameTime())
        .stadium(dto.getStadium())
        .remarks(dto.getRemarks())
        .boardse(dto.getBoardse())
        .awayTeam(dto.getAwayTeam())
        .awayTeamScore(dto.getAwayTeamScore())
        .homeTeam(dto.getHomeTeam())
        .homeTeamScore(dto.getHomeTeamScore())
        .victoryTeam(dto.getVictoryTeam())
        .year(dto.getYear())
        .month(dto.getMonth())
        .recordType(dto.getRecordType())
        .highlightUrl(dto.getHighlightUrl())
        .highlightThumb(dto.getHighlightThumb())
        .highlightSummary(dto.getHighlightSummary())
        .build();
    }
}
