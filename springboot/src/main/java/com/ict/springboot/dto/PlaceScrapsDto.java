package com.ict.springboot.dto;

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
public class PlaceScrapsDto {
	
	//찜 고유키
	private Long id;

    //찜한 유저의 고유키
    private Long userId;
    
    //찜한 장소의 고유키
    private Long placeId;
    
    //찜한 유저의 정보
    private UsersDto user;
    
}
