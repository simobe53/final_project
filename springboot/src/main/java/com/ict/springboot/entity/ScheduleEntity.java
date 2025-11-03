package com.ict.springboot.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KBO_SCHEDULE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "GAME_DATE")
    private LocalDate gameDate;

    @Column(name = "GAME_TIME", length = 5)
    private String gameTime;
    
    @Column(name = "STADIUM", length = 500)
    private String stadium;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "BOARDSE", length = 500)
    private String boardse;

    @Column(name = "AWAY_TEAM", length = 500)
    private String awayTeam;

    @Column(name = "AWAY_TEAM_SCORE", length= 3)
    private Integer awayTeamScore;

    @Column(name = "HOME_TEAM", length = 500)
    private String homeTeam;

    @Column(name = "HOME_TEAM_SCORE", length= 3)
    private Integer homeTeamScore;

    @Column(name = "VICTORY_TEAM", length = 500)
    private String victoryTeam;

    @Column(name = "YEAR", length = 4)
    private Integer year;
    
    @Column(name = "MONTH", length = 2)
    private Integer month;
    
    @Column(name = "RECORD_TYPE", length = 500)
    private String recordType;

    @Column(name = "HIGHLIGHT_URL", length = 500)
    private String highlightUrl;

    @Column(name = "HIGHLIGHT_THUMB", length = 500)
    private String highlightThumb;

    @Column(name = "HIGHLIGHT_SUMMARY", columnDefinition = "CLOB")
    private String highlightSummary;
    
}

