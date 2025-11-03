package com.ict.springboot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KBO_NEWS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsEntity {
    
    @Id
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "TITLE", length = 500)
    private String title;
    
    @Column(name = "LINK", length = 1000)
    private String link;
    
    @Column(name = "IMAGE_URL", length = 1000)
    private String imageUrl;
    
    @Lob
    @Column(name = "CONTENT")
    private String content;
    
    @Column(name = "TEAM_ID", length = 50)
    private String teamId;
    
    @Column(name = "TEAM_NAME", length = 50)
    private String teamName;
}

