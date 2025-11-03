package com.ict.springboot.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleRequestDto {
    private Long simulationId;   // 어떤 시뮬레이션에 대한 기사 생성인지
    private Object gameLog;      // FastAPI에 전달할 시뮬레이션 게임 기록
}
