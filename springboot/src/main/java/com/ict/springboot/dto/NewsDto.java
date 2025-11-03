package com.ict.springboot.dto;

import com.ict.springboot.entity.NewsEntity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDto {
    private Long id;
    private String title;
    private String link;
    private String imageUrl;
    private String content;
    private String teamId;
    private String teamName;
    
    public static NewsDto toDto(NewsEntity entity) {
        if (entity == null) {
            return null;
        }
        return NewsDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .link(entity.getLink())
                .imageUrl(entity.getImageUrl())
                .content(entity.getContent())
                .teamId(entity.getTeamId())
                .teamName(entity.getTeamName())
                .build();
    }
}

