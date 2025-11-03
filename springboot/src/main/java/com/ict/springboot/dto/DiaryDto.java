package com.ict.springboot.dto;

import java.util.List;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiaryDto {
    private Long id;
    private String ticketUrl;
    private List<String> photoUrls;
    private String content;
    private Long scheduleId;
    private Integer totalGames;
    private Integer totalWins;
}
