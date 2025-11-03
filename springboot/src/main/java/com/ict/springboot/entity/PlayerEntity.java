package com.ict.springboot.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PLAYERS")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity {
    
    @Id
    @Column(name = "p_no", length = 50, nullable = false)
    private Long pNo;
    
    @Column(name = "player_name", length = 50, nullable = false)
    private String playerName;
    
    @Column(name = "img_url", length = 500)
    private String imgUrl;
    
    @Column(name = "back_no", length = 50)
    private Integer backNo;
    
    @Column(name = "birth", length = 50)
    private String birth;
    
    @Column(name = "position", length = 100)
    private String position;
    
    @Column(name = "height", length = 50)
    private Double height;
    
    @Column(name = "weight", length = 50)
    private Double weight;
    
    @Lob
    @Column(name = "history", columnDefinition = "CLOB")
    private String history;
    
    @Column(name = "signing_fee", length = 100)
    private String signingFee;
    
    @Column(name = "salary", length = 100)
    private String salary;
    
    @Lob
    @Column(name = "draft", columnDefinition = "CLOB")
    private String draft;
    
    @Column(name = "join_year", length = 50)
    private String joinYear;
    
    // 추가 통계 정보
    @Column(name = "year")
    private Double year;
    
    @Column(name = "age")
    private Double age;
    
    @Column(name = "hand", length = 30)
    private String hand;
    
    @Column(name = "player_type", length = 30)
    private String playerType;
    
    // 타격 통계 (b_ 접두사)
    @Column(name = "b_ab")
    private Integer bAb;  // 타석
    
    @Column(name = "b_avg")
    private Double bAvg;  // 타율
    
    @Column(name = "b_obp")
    private Double bObp;  // 출루율
    
    @Column(name = "b_slg")
    private Double bSlg;  // 장타율
    
    @Column(name = "b_ops")
    private Double bOps;  // OPS
    
    @Column(name = "b_hr")
    private Integer bHr;  // 홈런
    
    @Column(name = "b_rbi")
    private Integer bRbi;  // 타점
    
    @Column(name = "b_sb")
    private Integer bSb;  // 도루
    
    @Column(name = "b_2B")
    private Integer b2B;  // 2루타
    
    @Column(name = "b_3B")
    private Integer b3B;  // 3루타
    
    @Column(name = "b_HP")
    private Integer bHp;  // 몸에 맞는 볼
    
    @Column(name = "b_GDP")
    private Integer bGdp;  // 병살타
    
    @Column(name = "b_SF")
    private Integer bSf;  // 희생플라이
    
    @Column(name = "b_SO")
    private Integer bSo;  // 삼진
    
    @Column(name = "b_ePA")
    private Integer bEpa;  // 유효 타석
    
    @Column(name = "b_BB")
    private Integer bBb;  // 볼넷
    
    @Column(name = "b_H")
    private Integer bH;  // 안타
    
    @Column(name = "b_IB")
    private Integer bIb;  // 고의사구
    
    @Column(name = "b_R")
    private Integer bR;  // 득점
    
    // 투수 통계 (p_ 접두사)
    @Column(name = "p_w")
    private Integer pW;  // 승
    
    @Column(name = "p_l")
    private Integer pL;  // 패
    
    @Column(name = "p_era")
    private Double pEra;  // 평균자책점
    
    @Column(name = "p_whip")
    private Double pWhip;  // WHIP
    
    @Column(name = "p_ip")
    private Double pIp;  // 이닝
    
    @Column(name = "p_so")
    private Integer pSo;  // 삼진
    
    @Column(name = "p_FIP")
    private Double pFip;  // FIP
    
    @Column(name = "p_2B")
    private Integer p2B;  // 피2루타
    
    @Column(name = "p_3B")
    private Integer p3B;  // 피3루타
    
    @Column(name = "p_HR")
    private Integer pHr;  // 피홈런
    
    @Column(name = "p_HP")
    private Integer pHp;  // 피몸에맞는볼
    
    @Column(name = "p_ROE")
    private Integer pRoe;  // 피실책
    
    @Column(name = "p_BB")
    private Integer pBb;  // 피볼넷
    
    @Column(name = "p_H")
    private Integer pH;  // 피안타
    
    @Column(name = "p_IB")
    private Integer pIb;  // 피고의사구
    
    @Column(name = "p_R")
    private Double pR;  // 피득점
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private TeamEntity team;
}
