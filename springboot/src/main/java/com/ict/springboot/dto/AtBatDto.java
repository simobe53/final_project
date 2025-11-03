package com.ict.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtBatDto {
    
    private Long id;
    private Long simulationId; // 시뮬레이션 ID (외래키)
    private String inningHalf; // 이닝/하프 (예: "1top", "1bottom")
    private String team; // 팀 ("home" 또는 "away")
    private Long pitcherPNo; // 투수 번호
    private Long batterPNo; // 타자 번호
    private Integer prevScoreHome; // 이전 홈팀 점수
    private Integer prevScoreAway; // 이전 어웨이팀 점수
    private Integer prevOuts; // 이전 아웃 수
    private Long prevBase1; // 이전 1루 주자 번호
    private Long prevBase2; // 이전 2루 주자 번호
    private Long prevBase3; // 이전 3루 주자 번호
    private String result; // 타석 결과
    private Integer rbi; // 타점
    private LocalDateTime createdAt;
}
