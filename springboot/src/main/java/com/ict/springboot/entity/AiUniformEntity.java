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
@Table(name = "AI_UNIFORMS")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUniformEntity {
    
    @Id
    @SequenceGenerator(name = "SEQ_AI_UNIFORM_GENERATOR", sequenceName = "SEQ_AI_UNIFORM", allocationSize = 1, initialValue = 1)
    @GeneratedValue(generator = "SEQ_AI_UNIFORM_GENERATOR", strategy = GenerationType.SEQUENCE)
    @Column(length = 20, nullable = false)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UsersEntity user;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private TeamEntity team;
    
    @Column(name = "korean_prompt", nullable = false, length = 1000)
    private String koreanPrompt;
    
    @Column(name = "english_prompt", nullable = false, length = 1000)
    private String englishPrompt;
    
    @Lob
    @Column(name = "image_data", columnDefinition = "BLOB")
    private byte[] imageData;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "filename", length = 200)
    private String filename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "image_size", length = 20)
    private String imageSize;
    
    @Column(name = "created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
}
