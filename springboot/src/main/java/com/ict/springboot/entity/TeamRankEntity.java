package com.ict.springboot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name="team_rank_2025")
@IdClass(TeamRankEntity.TeamRankId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRankEntity {

    @Id
    @Column(name = "YEAR")
    private String year;

    @Id
    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "rank", length = 10)
    private String rank;

    @Column(name = "games_played", length = 10)
    private String gamesPlayed;

    @Column(name = "wins", length = 10)
    private String wins;

    @Column(name = "losses", length = 10)
    private String losses;

    @Column(name = "ties", length = 10)
    private String ties;

    @Column(name = "win_pct", length = 10)
    private String winPct;

    @Column(name = "gb", length = 10)
    private String gb;

    @Column(name = "last_10_games", length = 20)
    private String last10Games;

    @Column(name = "streak", length = 20)
    private String streak;

    @Column(name = "home_record", length = 20)
    private String homeRecord;

    @Column(name = "away_record", length = 20)
    private String awayRecord;

    @Column(name = "RECORD_TYPE", length = 50)
    private String recordType;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamRankId implements Serializable {
        private String year;
        private String teamName;
    }
}

