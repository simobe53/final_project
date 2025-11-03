package com.ict.springboot.dto;

import java.time.LocalDateTime;

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
public class SimulationRequestDto {
    
    private String stadium;
    private String homeTeam;
    private String awayTeam;
    private LineupDto homeLineup;
    private LineupDto awayLineup;
    private LocalDateTime showAt;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineupDto {
        // 기존 ID 필드 (호환성을 위해 유지)
        private Long pitcher;
        private Long batting1;
        private Long batting2;
        private Long batting3;
        private Long batting4;
        private Long batting5;
        private Long batting6;
        private Long batting7;
        private Long batting8;
        private Long batting9;
    }
}
