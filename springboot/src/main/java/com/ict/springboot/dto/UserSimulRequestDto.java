package com.ict.springboot.dto;

import com.ict.springboot.entity.UserSimulRequestEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSimulRequestDto {

    private Long id;
    private UsersDto user;
    private Long hometeam;
    private Long awayteam;
    private String homeLineup;
    private String awayLineup;
    private String stadium;
    private String status; // PENDING, APPROVED, REFUSE
    private LocalDateTime requestAt;
    private LocalDateTime updateAt;
    private String adminComment;
    private Long simulationId;
    private LocalDateTime scheduledAt;

    // Entity -> DTO 변환
    public static UserSimulRequestDto toDto(UserSimulRequestEntity entity) {
        if (entity == null) return null;

        return UserSimulRequestDto.builder()
                .id(entity.getId())
                .user(UsersDto.toDto(entity.getUser()))
                .hometeam(entity.getHometeam())
                .awayteam(entity.getAwayteam())
                .homeLineup(entity.getHomeLineup())
                .awayLineup(entity.getAwayLineup())
                .stadium(entity.getStadium())
                .status(entity.getStatus())
                .requestAt(entity.getRequestAt())
                .updateAt(entity.getUpdateAt())
                .adminComment(entity.getAdminComment())
                .simulationId(entity.getSimulationId())
                .scheduledAt(entity.getScheduledAt())
                .build();
    }

    // DTO -> Entity 변환
    public UserSimulRequestEntity toEntity() {
        return UserSimulRequestEntity.builder()
                .id(this.id)
                .user(this.user != null ? this.user.toEntity() : null)
                .hometeam(this.hometeam)
                .awayteam(this.awayteam)
                .homeLineup(this.homeLineup)
                .awayLineup(this.awayLineup)
                .stadium(this.stadium)
                .status(this.status)
                .requestAt(this.requestAt)
                .updateAt(this.updateAt)
                .adminComment(this.adminComment)
                .simulationId(this.simulationId)
                .scheduledAt(this.scheduledAt)
                .build();
    }
}
