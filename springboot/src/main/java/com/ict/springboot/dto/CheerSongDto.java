package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.CheerSongEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "응원곡 정보")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheerSongDto {

    @Schema(description = "응원곡 ID", example = "1")
    private Long songId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

    @Schema(description = "팀 ID", example = "SK")
    private String teamId;

    @Schema(description = "팀 이름", example = "SK 와이번스")
    private String teamName;

    @Schema(description = "선수 이름", example = "김광현")
    private String playerName;

    @Schema(description = "곡 분위기", example = "힙합")
    private String mood;

    @Schema(description = "응원곡 제목", example = "SK 파이팅!")
    private String title;

    @Schema(description = "응원곡 가사")
    private String lyrics;

    @Schema(description = "오디오 URL", example = "https://example.com/audio.mp3")
    private String audioUrl;

    @Schema(description = "재생 시간 (초)", example = "180")
    private Integer duration;

    @Schema(description = "공유 여부", example = "false")
    private Boolean isShared;

    @Schema(description = "생성 방식", example = "AI_GENERATED", allowableValues = {"AI_GENERATED", "YOUTUBE_COVER"})
    private String sourceType;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    // Entity → DTO 변환
    public static CheerSongDto toDto(CheerSongEntity entity) {
        if (entity == null) {
            return null;
        }

        return CheerSongDto.builder()
            .songId(entity.getSongId())
            .userId(entity.getUser() != null ? entity.getUser().getId() : null)
            .userName(entity.getUser() != null ? entity.getUser().getName() : null)
            .teamId(entity.getTeam() != null ? entity.getTeam().getIdKey() : null)
            .teamName(entity.getTeam() != null ? entity.getTeam().getName() : null)
            .playerName(entity.getPlayerName())
            .mood(entity.getMood())
            .title(entity.getTitle())
            .lyrics(entity.getLyrics())
            .audioUrl(entity.getAudioUrl())
            .duration(entity.getDuration())
            .isShared(entity.getIsShared())
            .sourceType(entity.getSourceType())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    // DTO → Entity 변환
    public static CheerSongEntity toEntity(CheerSongDto dto, UsersEntity user, TeamEntity team) {
        return CheerSongEntity.builder()
            .user(user)
            .team(team)
            .playerName(dto.getPlayerName())
            .mood(dto.getMood())
            .title(dto.getTitle())
            .lyrics(dto.getLyrics())
            .audioUrl(dto.getAudioUrl())
            .duration(dto.getDuration())
            .isShared(dto.getIsShared() != null ? dto.getIsShared() : false)
            .sourceType(dto.getSourceType() != null ? dto.getSourceType() : "AI_GENERATED")
            .build();
    }
}
