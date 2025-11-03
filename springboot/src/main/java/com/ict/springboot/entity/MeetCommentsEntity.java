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
@Table(name = "MEET_COMMENTS")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetCommentsEntity {
    
    @Id
    @SequenceGenerator(name = "SEQ_MEET_COMMENTS_GENERATOR", sequenceName = "SEQ_MEET_COMMENTS",initialValue = 1,allocationSize = 1)
    @GeneratedValue(generator = "SEQ_MEET_COMMENTS_GENERATOR",strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(length = 1000,nullable = false)
    private String content;

    @CreationTimestamp
    @ColumnDefault("SYSDATE")
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",foreignKey = @ForeignKey(name= "FK_MEET_COMMENTS_USERS"))
    private UsersEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meet_id",foreignKey = @ForeignKey(name= "FK_MEET_COMMENTS_MEETS"))
    private MeetsEntity meet;
    
}

