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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SIMULATIONS")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationEntity {
    
    @Id
    @SequenceGenerator(name = "SEQ_SIMULATION_GENERATOR", sequenceName = "SEQ_SIMULATION", allocationSize = 1, initialValue = 1)
    @GeneratedValue(generator = "SEQ_SIMULATION_GENERATOR", strategy = GenerationType.SEQUENCE)
    @Column(length = 20, nullable = false)
    private Long id;
    
    @Column(name = "hometeam", nullable = false)
    private Long hometeam;
    
    @Column(name = "awayteam", nullable = false)
    private Long awayteam;
    
    @Lob
    @Column(name = "homeLineup", columnDefinition = "CLOB")
    private String homeLineup;
    
    @Lob
    @Column(name = "awayLineup", columnDefinition = "CLOB")
    private String awayLineup;
    
    @Column(name = "match_id", length = 30)
    private String matchId;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UsersEntity user;
    
    @Column(name = "show_at")
    private LocalDateTime showAt;

    @Column
    @ColumnDefault("0")
    private Boolean isFinished;
    
    @Column(name = "created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
