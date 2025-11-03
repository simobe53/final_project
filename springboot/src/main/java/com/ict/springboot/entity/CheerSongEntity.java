package com.ict.springboot.entity;

import java.time.LocalDateTime;

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
@Table(name = "CHEER_SONGS")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "cheer_song_seq_gen", sequenceName = "cheer_song_seq", initialValue = 1, allocationSize = 1)
public class CheerSongEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cheer_song_seq_gen")
    @Column(name = "song_id")
    private Long songId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UsersEntity user;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "mood", length = 100)
    private String mood;

    @Column(name = "title", length = 200)
    private String title;

    @Lob
    @Column(name = "lyrics", columnDefinition = "CLOB")
    private String lyrics;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "is_shared")
    private Boolean isShared;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
