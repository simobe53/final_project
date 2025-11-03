package com.ict.springboot.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUniformDto {
    
    private Long id;
    private Long userId;
    private Long teamId;
    private String koreanPrompt;
    private String englishPrompt;
    private String imageUrl;
    private String filename;
    private Long fileSize;
    private String imageSize;
    private LocalDateTime createdAt;
    
    // Base64 인코딩된 이미지 데이터 (응답용)
    private String imageBase64;
    
}
