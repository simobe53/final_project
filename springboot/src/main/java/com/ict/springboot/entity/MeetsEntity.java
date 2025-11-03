package com.ict.springboot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MEETS")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MeetsEntity {

    @Id
    @SequenceGenerator(name = "SEQ_MEETS_GENERATOR",sequenceName = "SEQ_MEETS",initialValue = 1,allocationSize = 1)
    @GeneratedValue(generator = "SEQ_MEETS_GENERATOR",strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(length = 50,nullable = false)
    private String title;

    @Column(length = 1000,nullable = false)
    private String content;

    @Column(nullable = false)
    private int goal;

    @Column(name = "meet_at",nullable = false)
    private LocalDateTime meetAt;

    @CreationTimestamp
    @ColumnDefault("SYSDATE")
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_active",nullable = false)
    private boolean isActive = true;

    
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",foreignKey = @ForeignKey(name = "FK_MEETS_USERS"))
    private UsersEntity user;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id",foreignKey = @ForeignKey(name = "FK_MEETS_TEAMS"))
    private TeamEntity team;

}
