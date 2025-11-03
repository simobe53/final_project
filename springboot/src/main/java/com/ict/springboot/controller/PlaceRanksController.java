package com.ict.springboot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.PlaceRanksDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.PlaceRanksService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranks")
public class PlaceRanksController {
	private final PlaceRanksService rankService;
	// private final NotificationsService notifyService;

	//session에서 현재 로그인 한 유저의 고유키, 누른 장소의 고유키, 평점과 리뷰로 구성한다.(생성)
	@PostMapping("/create")
	public ResponseEntity<?> createRank(@RequestBody PlaceRanksDto placeRanks, HttpServletRequest request) {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");
		if (loginUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		// notifyService.createNotify(placeRanks.getPlaceId(), loginUser, type.PLACE_RANKS);
		return rankService.createRank(placeRanks);
	}
	
	//이미 작성한 리뷰가 있는지 확인하고 boolean 값으로 반환한다.(버튼 표시용)
	@GetMapping("/exists")
	public boolean existsRank(Long userId, Long placeId) {
		return rankService.existsRank(userId, placeId);
	}

	//장소에 맞춰서 리뷰를 불러온다.(조회)
	@GetMapping("/check")
	public List<PlaceRanksDto> checkRank(@RequestParam Long placeId) {
		return rankService.checkRank(placeId);
	}

	//리뷰 점수의 평균을 구해서 반환한다.
	@GetMapping("/average")
	public String getRankAverage(Long placeId) {
		return rankService.getRankAverage(placeId);
	}

	//리뷰 삭제
	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteRank(HttpServletRequest request, @RequestParam Long rankId) {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");
		if (loginUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		return rankService.deleteRank(loginUser, rankId);
	}
	
	//마이페이지에서 리뷰 조회
	@GetMapping("myPage")
	public ResponseEntity<?> myPage(HttpServletRequest request) {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");
		if (loginUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		return ResponseEntity.ok(rankService.myPage(loginUser));
	}

}