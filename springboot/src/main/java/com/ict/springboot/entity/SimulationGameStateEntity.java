package com.ict.springboot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SIMULATION_GAME_STATE")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationGameStateEntity {

    @Id
    @SequenceGenerator(name = "SEQ_SIM_GAME_STATE_GENERATOR", sequenceName = "SEQ_SIM_GAME_STATE", allocationSize = 1, initialValue = 1)
    @GeneratedValue(generator = "SEQ_SIM_GAME_STATE_GENERATOR", strategy = GenerationType.SEQUENCE)
    @Column(length = 20, nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "simulation_id", nullable = false)
    private SimulationEntity simulation;

    // 게임 진행 상태
    @Column(name = "inning", nullable = false)
    @ColumnDefault("1")
    private Integer inning;

    @Column(name = "half", length = 10, nullable = false)
    @ColumnDefault("'초'")
    private String half; // "초" 또는 "말"

    @Column(name = "outs", nullable = false)
    @ColumnDefault("0")
    private Integer outs;

    // 주자 상황 (p_no 저장)
    @Column(name = "base_1")
    private Long base1; // 1루 주자

    @Column(name = "base_2")
    private Long base2; // 2루 주자

    @Column(name = "base_3")
    private Long base3; // 3루 주자

    // 점수
    @Column(name = "home_score", nullable = false)
    @ColumnDefault("0")
    private Integer homeScore;

    @Column(name = "away_score", nullable = false)
    @ColumnDefault("0")
    private Integer awayScore;

    // 현재 타자 순번 - 홈/어웨이 각각 관리 (0-8)
    @Column(name = "home_batter_idx", nullable = false)
    @ColumnDefault("0")
    private Integer homeBatterIdx;

    @Column(name = "away_batter_idx", nullable = false)
    @ColumnDefault("0")
    private Integer awayBatterIdx;

    // 다음 타석에 나올 선수 정보 (타석 대기 상태 표시용)
    @Column(name = "current_pitcher_p_no")
    private Long currentPitcherPNo; // 현재 마운드에 있는 투수

    @Column(name = "next_batter_p_no")
    private Long nextBatterPNo; // 다음에 타석에 들어설 타자

    // 게임 상태
    @Column(name = "game_status", length = 20, nullable = false)
    @ColumnDefault("'READY'")
    private String gameStatus; // READY, PLAYING, FINISHED

    @Column(name = "winner", length = 10)
    private String winner; // "HOME", "AWAY", "TIE"

    // 메타 정보
    @Column(name = "created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}