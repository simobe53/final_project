package com.ict.springboot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ict.springboot.dto.PlacesDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.PlaceScrapsEntity;
import com.ict.springboot.repository.PlaceScrapsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceScrapsService {
	
	private final PlaceScrapsRepository placeScrapsRepository;
	private final PlacesService placesService;

	//찜한 장소이면 삭제하고 아니면 추가한다.
	@Transactional
	public boolean toggleScrap(UsersDto loginUser, Long placeId) {
		if (loginUser == null) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}

		//로그인한 유저가 찜한 장소인지 확인한다.
		PlaceScrapsEntity scrap = placeScrapsRepository.findByUserIdAndPlaceId(loginUser.getId(), placeId);

		//이미 찜한 상태라면 삭제 처리
		if(scrap != null) {
			placeScrapsRepository.delete(scrap);
			return false;
		}
		else {
			//스크랩 정보를 엔티티에 저장
			PlaceScrapsEntity scrapsEntity = PlaceScrapsEntity.builder()
												.userId(loginUser.getId())
												.placeId(placeId)
												.build();
			placeScrapsRepository.save(scrapsEntity);
			return true;
		}
	}

	//로그인한 유저가 찜한 장소인지 확인한다.
	public boolean existsScrap(UsersDto loginUser, Long placeId) {
		if (loginUser == null) {
			return false;
		}
		return placeScrapsRepository.existsByUserIdAndPlaceId(loginUser.getId(), placeId);
	}
	
	//현재 장소를 몇명이 찜했는지 int로 반환한다.
	public int countScrap(Long placeId) {
		return placeScrapsRepository.countByPlaceId(placeId);
	}
	
	//현재 장소를 몇명이 찜했는지 Long으로 반환한다 (관리자용)
	public Long getScrapCountByPlaceId(Long placeId) {
		return (long) countScrap(placeId);
	}
	

	// 사용자가 스크랩한 플레이스 목록 조회
	public List<PlacesDto> getMyScrapPlaces(Long userId) {
		try {
			List<PlaceScrapsEntity> scraps = placeScrapsRepository.findByUserId(userId);
			
			// 스크랩된 플레이스 ID 목록을 가져와서 PlacesDto로 변환
			return scraps.stream()
				.map(scrap -> placesService.getById(scrap.getPlaceId()))
				.filter(place -> place != null)
				.collect(Collectors.toList());
		} catch (Exception e) {
			System.err.println("getMyScrapPlaces 에러: " + e.getMessage());
			return new ArrayList<>();
		}
	}

}
