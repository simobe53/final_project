package com.ict.springboot.dto;

import com.ict.springboot.entity.PlayerEntity;
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
public class PlayerDto {
    
    private Long pNo;
    private String playerName;
    private String imgUrl;
    private Integer backNo;
    private String birth;
    private String position;
    private Double height;
    private Double weight;
    private String history;
    private String signingFee;
    private String salary;
    private String draft;
    private String joinYear;
    
    // 추가 통계 정보
    private Double year;
    private Double age;
    private String hand;
    private String playerType;
    
    // 타격 통계 객체
    private BattingStats battingStats;
    
    // 투수 통계 객체
    private PitchingStats pitchingStats;
    
    // 타격 통계 내부 클래스
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BattingStats {
        private Integer ab;  // 타석
        private Double avg;  // 타율
        private Double obp;  // 출루율
        private Double slg;  // 장타율
        private Double ops;  // OPS
        private Integer hr;  // 홈런
        private Integer rbi;  // 타점
        private Integer sb;  // 도루
    }
    
    // 투수 통계 내부 클래스
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PitchingStats {
        private Integer w;  // 승
        private Integer l;  // 패
        private Double era;  // 평균자책점
        private Double whip;  // WHIP
        private Double ip;  // 이닝
        private Integer so;  // 삼진
    }
    
    private String teamIdKey;  // 팀 코드 (KT, LG 등)
    private String teamName;   // 팀 이름 (KT wiz, LG 트윈스 등)
    
    // Entity → DTO 변환 메서드
    public static PlayerDto toDto(PlayerEntity entity) {
        if (entity == null) {
            return null;
        }
        
        // 타격 통계 객체 생성
        BattingStats battingStats = null;
        if (entity.getBAb() != null || entity.getBAvg() != null || entity.getBObp() != null || 
            entity.getBSlg() != null || entity.getBOps() != null || entity.getBHr() != null || 
            entity.getBRbi() != null || entity.getBSb() != null) {
            battingStats = BattingStats.builder()
                .ab(entity.getBAb())
                .avg(entity.getBAvg())
                .obp(entity.getBObp())
                .slg(entity.getBSlg())
                .ops(entity.getBOps())
                .hr(entity.getBHr())
                .rbi(entity.getBRbi())
                .sb(entity.getBSb())
                .build();
        }
        
        // 투수 통계 객체 생성
        PitchingStats pitchingStats = null;
        if (entity.getPW() != null || entity.getPL() != null || entity.getPEra() != null || 
            entity.getPWhip() != null || entity.getPIp() != null || entity.getPSo() != null) {
            pitchingStats = PitchingStats.builder()
                .w(entity.getPW())
                .l(entity.getPL())
                .era(entity.getPEra())
                .whip(entity.getPWhip())
                .ip(entity.getPIp())
                .so(entity.getPSo())
                .build();
        }
        
        return PlayerDto.builder()
            .pNo(entity.getPNo())
            .playerName(entity.getPlayerName())
            .imgUrl(entity.getImgUrl())
            .backNo(entity.getBackNo())
            .birth(entity.getBirth())
            .position(entity.getPosition())
            .height(entity.getHeight())
            .weight(entity.getWeight())
            .history(entity.getHistory())
            .signingFee(entity.getSigningFee())
            .salary(entity.getSalary())
            .draft(entity.getDraft())
            .joinYear(entity.getJoinYear())
            // 추가 통계 정보
            .year(entity.getYear())
            .age(entity.getAge())
            .hand(entity.getHand())
            .playerType(entity.getPlayerType())
            // 통계 객체
            .battingStats(battingStats)
            .pitchingStats(pitchingStats)
            .teamIdKey(entity.getTeam() != null ? entity.getTeam().getIdKey() : null)
            .teamName(entity.getTeam() != null ? entity.getTeam().getName() : null)
            .build();
    }
    // DTO → Entity 변환 메서드는 현시점 불필요
}
