package com.ict.springboot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatbotPlayerDataDto {
    private String playerName;
    private String teamName;
    private String position;

     // 포지션별 스탯
    private PitcherStats pitcherStats;
    private BatterStats batterStats;

    // 투수 스탯
    @Getter
    @Builder
    public static class PitcherStats {
        private Double era;                 // 평균자책점
        private Integer wins;               // 승리
        private Integer losses;             // 패배
        private Integer strikeouts;         // 탈삼진
        private Double whip;                // 이닝당 출루 허용률
        private Double fip;                 // 수비무관 평균자책점
        private Double inningsPitched;      // 투구 이닝
        private Integer hitsAllowed;        // 피안타
        private Integer homeRunsAllowed;    // 피홈런
        private Integer walksAllowed;       // 피볼넷
    }

    // 타자 스탯
    @Getter
    @Builder
    public static class BatterStats {
        private Double battingAverage; // 타율
        private Integer homeRuns;      // 홈런
        private Integer rbi;           // 타점
        private Double ops;            // OPS
        private Integer stolenBases;   // 도루
        private Integer atBats;        // 타석
        private Integer hits;          // 안타
        private Integer runs;          // 득점
        private Integer walks;         // 볼넷
        private Integer strikeouts;    // 삼진
        private Integer doubles;       // 2루타
        private Integer triples;       // 3루타
        private Integer doublePlays;   // 병살타
        private Integer sacrificeFly;  // 희생플라이
    }


}