package com.ict.springboot.dto;

import com.ict.springboot.entity.TeamRankEntity;

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
public class TeamRankDto {
    private String year;
    private String teamName;
    private String rank;
    private String gamesPlayed;
    private String wins;
    private String losses;
    private String ties;
    private String winPct;
    private String gb;
    private String last10Games;
    private String streak;
    private String homeRecord;
    private String awayRecord;
    private String recordType;

    public TeamRankEntity toEntity() {
        return TeamRankEntity.builder()
            .year(year)
            .teamName(teamName)
            .rank(rank)
            .gamesPlayed(gamesPlayed)
            .wins(wins)
            .losses(losses)
            .ties(ties)
            .winPct(winPct)
            .gb(gb)
            .last10Games(last10Games)
            .streak(streak)
            .homeRecord(homeRecord)
            .awayRecord(awayRecord)
            .recordType(recordType)
            .build();
    }

    public static TeamRankDto toDto(TeamRankEntity entity) {
        if (entity == null) {
            return null;
        }
        return TeamRankDto.builder()
            .year(entity.getYear())
            .teamName(entity.getTeamName())
            .rank(entity.getRank())
            .gamesPlayed(entity.getGamesPlayed())
            .wins(entity.getWins())
            .losses(entity.getLosses())
            .ties(entity.getTies())
            .winPct(entity.getWinPct())
            .gb(entity.getGb())
            .last10Games(entity.getLast10Games())
            .streak(entity.getStreak())
            .homeRecord(entity.getHomeRecord())
            .awayRecord(entity.getAwayRecord())
            .recordType(entity.getRecordType())
            .build();
    }
}

