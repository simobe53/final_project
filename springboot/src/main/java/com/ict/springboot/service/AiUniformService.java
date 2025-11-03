package com.ict.springboot.service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.AiUniformDto;
import com.ict.springboot.entity.AiUniformEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.AiUniformRepository;
import com.ict.springboot.repository.TeamRepository;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiUniformService {
    
    private final AiUniformRepository aiUniformRepository;
    private final UsersRepository usersRepository;
    private final TeamRepository teamRepository;
    
    /**
     * AI 유니폼 이미지 저장
     */
    public AiUniformDto saveAiUniform(AiUniformDto dto) {
        UsersEntity user = usersRepository.findById(dto.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        TeamEntity team = null;
        if (dto.getTeamId() != null) {
            team = teamRepository.findById(dto.getTeamId())
                .orElse(null);
        }
        
        AiUniformEntity entity = AiUniformEntity.builder()
            .user(user)
            .team(team)
            .koreanPrompt(dto.getKoreanPrompt())
            .englishPrompt(dto.getEnglishPrompt())
            .imageUrl(dto.getImageUrl())
            .filename(dto.getFilename())
            .fileSize(dto.getFileSize())
            .imageSize(dto.getImageSize())
            .build();
        
        // Base64 이미지 데이터를 BLOB으로 저장
        if (dto.getImageBase64() != null && !dto.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(dto.getImageBase64());
                entity.setImageData(imageBytes);
            } catch (Exception e) {
                throw new RuntimeException("이미지 데이터 변환 실패: " + e.getMessage());
            }
        }
        
        AiUniformEntity savedEntity = aiUniformRepository.save(entity);
        return convertToDto(savedEntity);
    }
    
    /**
     * 사용자별 AI 유니폼 목록 조회
     */
    public List<AiUniformDto> getAiUniformsByUserId(Long userId) {
        List<AiUniformEntity> entities = aiUniformRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return entities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * 팀별 AI 유니폼 목록 조회
     */
    public List<AiUniformDto> getAiUniformsByTeamId(Long teamId) {
        List<AiUniformEntity> entities = aiUniformRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
        return entities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * 파일명으로 AI 유니폼 조회
     */
    public Optional<AiUniformDto> getAiUniformByFilename(String filename) {
        Optional<AiUniformEntity> entity = Optional.ofNullable(aiUniformRepository.findByFilename(filename));
        return entity.map(this::convertToDto);
    }
    
    /**
     * AI 유니폼 삭제
     */
    public void deleteAiUniform(Long id) {
        aiUniformRepository.deleteById(id);
    }
    
    /**
     * Entity를 DTO로 변환
     */
    private AiUniformDto convertToDto(AiUniformEntity entity) {
        AiUniformDto dto = AiUniformDto.builder()
            .id(entity.getId())
            .userId(entity.getUser().getId())
            .teamId(entity.getTeam() != null ? entity.getTeam().getId() : null)
            .koreanPrompt(entity.getKoreanPrompt())
            .englishPrompt(entity.getEnglishPrompt())
            .imageUrl(entity.getImageUrl())
            .filename(entity.getFilename())
            .fileSize(entity.getFileSize())
            .imageSize(entity.getImageSize())
            .createdAt(entity.getCreatedAt())
            .build();
        
        // BLOB 데이터를 Base64로 변환
        if (entity.getImageData() != null) {
            String base64Image = Base64.getEncoder().encodeToString(entity.getImageData());
            dto.setImageBase64(base64Image);
        }
        
        return dto;
    }
    
}
