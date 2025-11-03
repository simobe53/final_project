package com.ict.springboot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ARTICLES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleEntity {

    @Id
    @SequenceGenerator(
        name = "SEQ_ARTICLE_GENERATOR",
        sequenceName = "SEQ_ARTICLE",
        allocationSize = 1,
        initialValue = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "SEQ_ARTICLE_GENERATOR"
    )
    @Column(nullable = false)
    private Long id;

    // FK: SIMULATION_ID → SimulationEntity.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "SIMULATION_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_ARTICLE_SIMULATION")
    )
    private SimulationEntity simulation;

    @Column(name = "TEAM_NAME", nullable = false, length = 100)
    private String teamName;

    @Lob
    @Column(name = "CONTENT", columnDefinition = "CLOB")
    private String content;
}