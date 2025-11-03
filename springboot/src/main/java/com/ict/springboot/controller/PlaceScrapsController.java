package com.ict.springboot.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.PlaceScrapsService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scrap")
public class PlaceScrapsController {

	private final PlaceScrapsService scrapsService;
	// private final NotificationsService notifyService;
	
	//이미 찜한 장소인지 확인하고 boolean 값으로 반환한다.(버튼 표시용)
	@GetMapping("/toggle")
	public ResponseEntity<?> toggleScrap(HttpServletRequest request, @RequestParam Long placeId) {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");
		if (loginUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		// notifyService.createNotify(placeId, loginUser, type.PLACE_SCRAP);
		return ResponseEntity.ok(scrapsService.toggleScrap(loginUser, placeId));
	}

	//이미 찜한 장소인지 확인하고 boolean 값으로 반환한다.(새로고침 방지)
	@GetMapping("exists")
	public ResponseEntity<?> exists(HttpServletRequest request, @RequestParam Long placeId) {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");
		if (loginUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		return ResponseEntity.ok(scrapsService.existsScrap(loginUser, placeId));
	}

	//해당 장소를 몇명이 찜했는지 반환한다.
	@GetMapping("/count")
	public int countScrap(@RequestParam Long placeId) {
		return scrapsService.countScrap(placeId);
	}
	
	//내가 찜한 장소를 반환한다.
	@GetMapping("/myPage")
	public ResponseEntity<?> myPage(HttpServletRequest request) {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");
		if (loginUser == null) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		return ResponseEntity.ok(scrapsService.getMyScrapPlaces(loginUser.getId()));
	}
}