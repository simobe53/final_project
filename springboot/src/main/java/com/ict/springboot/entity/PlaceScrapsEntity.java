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
@Table(name = "PLACE_SCRAP")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceScrapsEntity {
    
    //찜 고유키
    @Id
    @SequenceGenerator(name = "SCRAP_SEQ_GEN", sequenceName = "SCRAP_SEQ", initialValue = 1)
    @GeneratedValue(generator = "SCRAP_SEQ_GEN",strategy = GenerationType.SEQUENCE)
    @Column(length = 20, nullable = false)
    private Long id;

    //찜한 유저의 아이디
    @Column(length = 20, nullable = false)
    private Long userId;
    
    //찜한 장소의 아이디
    @Column(length = 20, nullable = false)
    private Long placeId;

    //찜한 시점
    @Column(name="created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
