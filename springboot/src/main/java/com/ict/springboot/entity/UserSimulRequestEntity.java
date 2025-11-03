package com.ict.springboot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_simul_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSimulRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_simul_request_seq_gen")
    @SequenceGenerator(name = "user_simul_request_seq_gen", sequenceName = "user_simul_request_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UsersEntity user;

    @Column(name = "hometeam", nullable = false)
    private Long hometeam;

    @Column(name = "awayteam", nullable = false)
    private Long awayteam;

    @Lob
    @Column(name = "home_lineup", columnDefinition = "CLOB")
    private String homeLineup;

    @Lob
    @Column(name = "away_lineup", columnDefinition = "CLOB")
    private String awayLineup;

    @Column(name = "stadium")
    private String stadium;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REFUSE, SCHEDULED,EXECUTED,FAILED

    @Column(name = "request_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime requestAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Column(name = "admin_comment")
    private String adminComment;

    @Column(name = "simulation_id")
    private Long simulationId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
}
