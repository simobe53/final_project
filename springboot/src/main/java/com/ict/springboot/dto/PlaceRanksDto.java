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
@AllArgsConstructor
@NoArgsConstructor
public class PlaceRanksDto {
	
	//평점 고유키
	private Long id;

    //평가한 유저 고유키
    private Long userId;
    
    //평가한 장소 고유키
    private Long placeId;
    
    //유저 정보 담을 dto
	private UsersDto user;
    
    //평가한 점수
    private double rank;

    //평가한 내용
    private String comments;
    
    //장소 정보를 담을 dto
    private PlacesDto place;
    
    //평가한 시점
    private LocalDateTime createdAt;
}