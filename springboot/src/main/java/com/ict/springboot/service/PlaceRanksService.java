package com.ict.springboot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ict.springboot.dto.PlaceRanksDto;
import com.ict.springboot.dto.PlacesDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.PlaceRanksEntity;
import com.ict.springboot.entity.PlacesEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.PlaceRanksRepository;
import com.ict.springboot.repository.PlacesRepository;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceRanksService {

	private final PlaceRanksRepository placeRanksRepository;
	private final PlacesRepository placeRepository;
	private final UsersRepository usersRepository;

	//session에서 현재 로그인 한 유저의 고유키, 누른 장소의 고유키, 평점과 리뷰로 구성한다.(생성)
	@Transactional
	public ResponseEntity<Void> createRank(PlaceRanksDto placeRanks) {
		
		//이미 리뷰가 있으면 작성하지 못하게 막는다.
		if(existsRank(placeRanks.getUserId(), placeRanks.getPlaceId())) return null;
	
		//리뷰를 엔티티에 저장
		PlaceRanksEntity rankEntity = PlaceRanksEntity.builder()
											.userId(placeRanks.getUserId())
											.placeId(placeRanks.getPlaceId())
											.comments(placeRanks.getComments())
											.placeRank(placeRanks.getRank())
											.build();
											
		placeRanksRepository.save(rankEntity);

		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	//이미 작성한 리뷰가 있는지 확인하고 boolean 값으로 반환한다.
	public boolean existsRank(Long userId, Long placeId) {
		return placeRanksRepository.existsByUserIdAndPlaceId(userId, placeId);
	}

	//장소에 맞춰서 리뷰를 불러온다.(조회)
	public List<PlaceRanksDto> checkRank(Long placeId) {
		
		//해당 장소에 리뷰가 있으면 불러온다.
		List<PlaceRanksEntity> rankEntity = placeRanksRepository.findByPlaceId(placeId);

		//없는 경우에는 빈 리스트를 반환한다.
		if(rankEntity.isEmpty()) return new ArrayList<>();

		//리뷰 조회하기
		List<PlaceRanksDto> rankDto = rankEntity.stream().map(entity -> {

			//평가한 유저의 정보를 얻기위한 엔티티
			UsersEntity user = usersRepository.findById(entity.getUserId()).orElse(null);
			
			UsersDto userDto = UsersDto.builder().name(user.getName()).account(user.getAccount()).build();
			
			//프론트로 보낼 dto 빌드
			return PlaceRanksDto.builder()
					.id(entity.getId())
					.placeId(placeId)
					.userId(user.getId())
					.user(userDto)
					.rank(entity.getPlaceRank())
					.comments(entity.getComments())
					.createdAt(entity.getCreatedAt())
					.build();
		}).sorted((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt())).collect(Collectors.toList());

		return rankDto;
	}

	//리뷰 점수의 평균을 구해서 반환한다.
	public String getRankAverage(Long placeId) {
		
		//해당 플레이스의 평점이 있는지 찾아서 리스트에 넣는다.
		List<PlaceRanksEntity> ranks = placeRanksRepository.findByPlaceId(placeId);

		//평점을 계산해서 average에 넣는다.
		double average = ranks.stream().mapToDouble(PlaceRanksEntity::getPlaceRank).average().orElse(0.0); //없는 경우 0.0 반환

		//0.0까지 반환
		return String.format("%.1f", average);
	}

	//장소 ID로 리뷰 목록을 조회한다 (관리자용)
	public List<PlaceRanksDto> getRanksByPlaceId(Long placeId) {
		return checkRank(placeId);
	}

	//리뷰 삭제
	public ResponseEntity<Void> deleteRank(UsersDto loginUser, Long rankId) {
		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		//리뷰 아이디로 해당 리뷰 가져오기
		PlaceRanksEntity rank = placeRanksRepository.findById(rankId).orElse(null);

		//해당 리뷰가 없을 때 예외처리
		if(rank == null) return ResponseEntity.notFound().build();

		//리뷰의 작성자와 로그인한 유저가 같은지 확인하고 삭제
		if(rank.getUserId() == loginUser.getId()) placeRanksRepository.delete(rank);

		return ResponseEntity.noContent().build();
	}
	
	//마이 페이지에는 로그인 한 유저가 작성한 평점만 보여준다.
	public List<PlaceRanksDto> myPage(UsersDto loginUser) {
		if (loginUser == null) {
			return new ArrayList<>();
		}

		//로그인한 유저가 작성한 평점만 가져온다.
		List<PlaceRanksEntity> rankEntity = placeRanksRepository.findByUserId(loginUser.getId());
		
		List<PlaceRanksDto> rankDto = rankEntity.stream().map(entity -> {
			PlacesEntity place = placeRepository.findById(entity.getPlaceId()).orElse(null);
			if (place == null) return null;
			
			PlacesDto placeDto = PlacesDto.builder()
					.id(place.getId())
					.name(place.getName())
					.image(place.getImage())
					.category(place.getCategory())
					.address(place.getAddress()).build();
			
			return PlaceRanksDto.builder()
					.id(entity.getId())
					.place(placeDto)
					.rank(entity.getPlaceRank())
					.createdAt(entity.getCreatedAt())
					.comments(entity.getComments())
					.build();
					
		}).filter(dto->dto!=null).collect(Collectors.toList());
		
		return rankDto;
	}

	
}