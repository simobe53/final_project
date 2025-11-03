package com.ict.springboot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PLACE_RANKS")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRanksEntity {
    
    //평점 고유키
    @Id
    @SequenceGenerator(name = "RANK_SEQ_GEN", sequenceName = "RANK_SEQ", initialValue = 1)
    @GeneratedValue(generator = "RANK_SEQ_GEN",strategy = GenerationType.SEQUENCE)
    @Column(length = 20, nullable = false)
    private Long id;

    //평점을 달은 유저의 아이디
    @Column(length = 20, nullable = false)
    private Long userId;
    
    //평점이 달린 장소의 아이디
    @Column(length = 20, nullable = false)
    private Long placeId;

    //평점에 달려있는 평가
    @Column(length = 1000, nullable = false)
    private String comments;

    //평점을 저장하기 때문에 float
    @Column(length = 20, nullable = false)
    private double placeRank;
 
    //평가한 시점
    @Column(name="created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
