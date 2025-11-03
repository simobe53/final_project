package com.ict.springboot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

@Entity
@Table(name = "at_bats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtBatEntity {
    
    @Id
    @SequenceGenerator(name = "SEQ_AT_BATS_GENERATOR", sequenceName = "SEQ_AT_BATS", initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "SEQ_AT_BATS_GENERATOR", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false, foreignKey = @ForeignKey(name = "FK_AT_BATS_SIMULATION"))
    private SimulationEntity simulation; // 시뮬레이션 (외래키)

    @Column(name = "inning_half", nullable = false)
    private String inningHalf; // 이닝/하프 (예: "1top", "1bottom")
    
    @Column(name = "pitcher_p_no", nullable = false)
    private Long pitcherPNo; // 투수 번호
    
    @Column(name = "batter_p_no", nullable = false)
    private Long batterPNo; // 타자 번호

    @Column(name = "batting_order")
    private Integer battingOrder; // 타순

    @Column(name = "prev_score_home", nullable = false)
    private Integer prevScoreHome; // 이전 홈팀 점수
    
    @Column(name = "prev_score_away", nullable = false)
    private Integer prevScoreAway; // 이전 어웨이팀 점수
    
    @Column(name = "prev_outs", nullable = false)
    private Integer prevOuts; // 이전 아웃 수
    
    @Column(name = "prev_base_1")
    private Long prevBase1; // 이전 1루 주자 번호
    
    @Column(name = "prev_base_2")
    private Long prevBase2; // 이전 2루 주자 번호
    
    @Column(name = "prev_base_3")
    private Long prevBase3; // 이전 3루 주자 번호
    
    @Column(name = "result", nullable = false)
    private String result; // 타석 결과 (영어)

    @Column(name = "result_korean")
    private String resultKorean; // 타석 결과 (한국어)

    @Column(name = "rbi", nullable = false)
    private Integer rbi; // 타점 (이 타석에서 발생한 득점)

    // 타석 후 상황 (new_game_state)
    @Column(name = "new_score_home")
    private Integer newScoreHome; // 타석 후 홈팀 점수

    @Column(name = "new_score_away")
    private Integer newScoreAway; // 타석 후 어웨이팀 점수

    @Column(name = "new_outs")
    private Integer newOuts; // 타석 후 아웃 수

    @Column(name = "new_base_1")
    private Long newBase1; // 타석 후 1루 주자

    @Column(name = "new_base_2")
    private Long newBase2; // 타석 후 2루 주자

    @Column(name = "new_base_3")
    private Long newBase3; // 타석 후 3루 주자

    @Column(name = "probabilities", columnDefinition = "VARCHAR2(4000)")
    private String probabilities; // AI 예측 확률 (JSON)

    @Column(name = "created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
