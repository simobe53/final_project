package com.ict.springboot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MEET_APPLY", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "meet_id" }) })
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetApplyEntity {

    @Id
    @SequenceGenerator(name = "SEQ_MEET_APPLY_GENERATOR",sequenceName = "SEQ_MEET_APPLY",initialValue = 1,allocationSize = 1)
    @GeneratedValue(generator = "SEQ_MEET_APPLY_GENERATOR",strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(length = 1000,nullable = true)
    private String comments;

    // @Builder.Default // JPA에서 생성할때 default값
    // @Column(name = "is_approved",nullable = false)
    // private boolean isApproved = false;

    @CreationTimestamp
    @ColumnDefault("SYSDATE") //db에서 생성할때 default값
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at",nullable = true)
    private LocalDateTime approvedAt;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",foreignKey = @ForeignKey(name = "FK_MEET_APPLY_USERS"))
    private UsersEntity user;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "meet_id",foreignKey = @ForeignKey(name = "FK_MEET_APPLY_MEETS"))
    private MeetsEntity meet;
    
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
    @Builder.Default
    @Enumerated(EnumType.STRING)//EnumType.STRING을 써야 "PENDING" 같이 문자열로 DB에 저장됨
    @Column(name = "approval_status",nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;
    
}
