package com.ict.springboot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION_SENT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSentLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_log_seq_gen")
    @SequenceGenerator(name = "notification_log_seq_gen", sequenceName = "SEQ_NOTIFICATION_LOG", allocationSize = 1)
    private Long id;

    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "sent_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime sentAt;
}

