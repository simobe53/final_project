package com.ict.springboot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ict.springboot.dto.PlacesDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.PlacesEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.PlacesRepository;
import com.ict.springboot.repository.TeamRepository;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlacesService {

    private final UsersRepository usersRepo;
    private final PlacesRepository placesRepo;
    private final TeamRepository teamRepo;

    //전체 조회
    public List<PlacesDto> getAll(Map<String, String> params) {
        String teamId = "";
        
        if (!params.isEmpty()) {    // 서치파라미터 있을 시 팀에 한정되지 않도록 개발 진행
            teamId = params.get("teamId");
        }

        if (teamId.isEmpty()) {
            List<PlacesEntity> placesEntities = placesRepo.findAll();
            return placesEntities.stream().map(entity -> PlacesDto.toDto(entity)).collect(Collectors.toList());
        }
        TeamEntity team = teamRepo.findById(Long.valueOf(teamId)).orElseGet(()->null);
        List<PlacesEntity> placesEntities = placesRepo.findByTeamIdOrderByCreatedAtDesc(team.getId());
        return placesEntities.stream().map(entity -> PlacesDto.toDto(entity)).collect(Collectors.toList());
    }

    //상세 조회
    public PlacesDto getById(Long id) {
        Optional<PlacesEntity> placesEntity = placesRepo.findById(id); 

        return PlacesDto.toDto(placesEntity.orElseGet(() -> null));
    }

    //핫한 플레이스 조회 (메인용)
    public List<PlacesDto> getPlacesByRanksScraps(int limit, UsersDto loginUser, Long locationId) {
        if (loginUser == null) {
            return null;
        }
        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount()).orElseGet(()->null);
        if (userEntity == null) return null;
        List<PlacesEntity> placesEntities = placesRepo.findByTeamIdOrderByRanksAndScraps(locationId, limit);
        return placesEntities.stream().map(entity -> PlacesDto.toDto(entity)).collect(Collectors.toList());
    }


    //등록
    public PlacesDto create(PlacesDto dto) {
        boolean isDuplicated = placesRepo.existsByName(dto.getName());
        if(isDuplicated) return null;

        // team
        TeamEntity teamEntity = dto.getTeam().toEntity();

        // user
        Long userId = dto.getUser().getId();
        UsersEntity userEntity = usersRepo.findById(userId).orElseGet(()-> null);
        if (userEntity == null) return null;

        PlacesEntity placesEntity = PlacesEntity.builder()
            .user(userEntity)
            .name(dto.getName())
            .address(dto.getAddress())
            .image(dto.getImage())
            .category(dto.getCategory())
            .team(teamEntity)
            .build();

        placesEntity = placesRepo.save(placesEntity);
        return PlacesDto.toDto(placesEntity);
    }

    //수정
    public PlacesDto update(PlacesDto dto, long id){
        PlacesDto place = PlacesDto.toDto(placesRepo.findById(id).orElseGet(()->null));
        if(place == null) return null;//비어있는 경우 수정X
        //내용이 있는 경우 수정
        if(dto.getName() != null) place.setName(dto.getName());
        if(dto.getImage() != null) place.setImage(dto.getImage());
        if(dto.getAddress() != null) place.setAddress(dto.getAddress());
        if(dto.getTeam() != null) place.setTeam(dto.getTeam());
        if(dto.getCategory() != null) place.setCategory(dto.getCategory());
        PlacesEntity placesEntity = placesRepo.save(place.toEntity());

        return PlacesDto.toDto(placesEntity);
    }

    //삭제
    public PlacesDto delete(long id) throws Exception {
        PlacesEntity place = placesRepo.findById(id).orElseGet(()->null);
        if (place != null) {
            try{
                placesRepo.deleteById(id);
                return PlacesDto.toDto(place);
            }
            catch (Exception e) {
                throw new Exception("데이터 삭제에 문제가 생겼습니다.");
            }
        }
        return null;
    }
    
    //중복 조회
    public boolean checkExists(String name) {
        return placesRepo.existsByName(name);
    }

    // 관리자용 플레이스 조회 (페이지네이션 정보 포함)
    public Page<PlacesDto> getPlacesForAdminWithPagination(int page, int size, UsersDto loginUser) {
        if (loginUser == null) {
            return null;
        }
        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount()).orElseGet(()->null);
        if (userEntity == null) return null;

        Pageable pageable = PageRequest.of(page, size);
        Page<PlacesEntity> placesPage = placesRepo.findAllByOrderByCreatedAtDesc(pageable);
        
        Page<PlacesDto> placesDtoPage = placesPage.map(entity -> {
            PlacesDto dto = PlacesDto.toDto(entity);
            return dto;
        });
        
        return placesDtoPage;
    }

    // 관리자용 플레이스 검색 (페이지네이션 정보 포함)
    public Page<PlacesDto> searchPlacesForAdminWithPagination(String keyword, int page, int size, UsersDto loginUser) {
        if (loginUser == null) {
            return null;
        }
        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount()).orElseGet(()->null);
        if (userEntity == null) return null;

        Pageable pageable = PageRequest.of(page, size);
        Page<PlacesEntity> placesPage = placesRepo.findByNameOrAddressContainingOrderByCreatedAtDesc(keyword, pageable);
        
        Page<PlacesDto> placesDtoPage = placesPage.map(entity -> {
            PlacesDto dto = PlacesDto.toDto(entity);
            return dto;
        });
        
        return placesDtoPage;
    }

    // 메인 페이지용 상위 플레이스 조회
    public List<PlacesDto> getTopPlaces(int limit) {
        try {
            // 최근 생성된 플레이스들을 limit 개수만큼 가져오기
            Pageable pageable = PageRequest.of(0, limit);
            Page<PlacesEntity> placesPage = placesRepo.findAllByOrderByCreatedAtDesc(pageable);
            
            return placesPage.getContent().stream()
                .map(entity -> PlacesDto.toDto(entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("getTopPlaces 에러: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
