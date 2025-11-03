package com.ict.springboot.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ict.springboot.dto.MeetApplyDto;
import com.ict.springboot.dto.MeetsDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.MeetApplyEntity;
import com.ict.springboot.entity.MeetApplyEntity.ApprovalStatus;
import com.ict.springboot.entity.MeetsEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.MeetApplyRepository;
import com.ict.springboot.repository.MeetCommentsRepository;
import com.ict.springboot.repository.MeetsRepository;
import com.ict.springboot.repository.TeamRepository;
import com.ict.springboot.repository.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetsService {
    
    private final MeetsRepository meetRepo;
    private final UsersRepository userRepo;
    private final TeamRepository teamRepo;
    private final MeetApplyRepository meetApplyRepo;
    private final MeetCommentsRepository meetCommentsRepo;

    //<게시글 생성>
    public MeetsDto create(MeetsDto dto) {
        UsersEntity user = userRepo.findByAccount(dto.getUser().getAccount()).orElseGet(()->null);
        TeamEntity team = teamRepo.findById(dto.getTeam().getId()).orElseGet(()->null);
        MeetsEntity meetEntity = dto.toEntity();
        meetEntity.setUser(user);
        meetEntity.setTeam(team);
        return MeetsDto.toDto(meetRepo.save(meetEntity));
    }
    //<<전체 게시글 조회>
    public List<MeetsDto> meetAllByTeam(UsersDto loginUser) {
//        if (loginUser == null) {
//            System.out.println("세션에 사용자 정보가 없습니다.");
//            throw new IllegalStateException("로그인이 필요합니다.");
//            //ResponseEntity로 변경시
//            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
//        }
        //세션에 저장된 유저 아이디(로그인한 사용자 아이디)로 meet게시글 지원 목록 엔티티를 가져옴, 없을경우 []
        List<MeetApplyEntity> meetApplyEntities = loginUser != null ? meetApplyRepo.findAllByUserId(loginUser.getId()) : new ArrayList<>();
        //로그인유저가 지원한 게시글의 Id 목록
        List<Long> applyMeetIds = meetApplyEntities.stream().map(entity->entity.getMeet().getId()).collect(Collectors.toList());

        // 전체 모임 게시글 조회 (팀 필터링 제거)
        List<MeetsEntity>  meetEntities = meetRepo.findAll();

        return meetEntities.stream().map(entity->{
            MeetsDto meetDto = MeetsDto.toDto(entity);
            //로그인한 유저가 지원한 게시글 Id 목록에 포함되어 있으면 해당 객체의 Apply 값을 true
            meetDto.setApply(applyMeetIds.contains(entity.getId()));
            //로그인한 유저의 Id와 게시글 작성자의 Id가 일치하면 해당 객체의 Writer 값을 true
            meetDto.setWriter(loginUser != null && meetDto.getUser().getId() == loginUser.getId());
            meetDto.setTotalApply(meetApplyRepo.countByMeetId(entity.getId()));
            return meetDto;
            }).sorted((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt())).collect(Collectors.toList());
            //sorted((x,y)->Long.compare(y.getId(), x.getId()))  Id값으로 내림차순 정렬
    }
    //<전체 게시글 조회(페이지네이션)>
    public Page<MeetsDto> meetAllByUserTeam(UsersDto loginUser, Pageable pageable) {
//        if (loginUser == null) {
//            System.out.println("세션에 사용자 정보가 없습니다.");
//            throw new IllegalStateException("로그인이 필요합니다.");
//            //ResponseEntity로 변경시
//            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
//        }
        //세션에 저장된 유저 아이디(로그인한 사용자 아이디)로 meet게시글 지원 목록 엔티티를 가져옴, 없을경우 []
        List<MeetApplyEntity> meetApplyEntities = loginUser != null ? meetApplyRepo.findAllByUserId(loginUser.getId()) : new ArrayList<>();
        //로그인유저가 지원한 게시글의 Id 목록
        List<Long> applyMeetIds = meetApplyEntities.stream().map(entity->entity.getMeet().getId()).collect(Collectors.toList());

        Pageable newPageable = PageRequest.of(pageable.getPageNumber()-1, pageable.getPageSize(), pageable.getSort());
        // 전체 모임 게시글 조회 (팀 필터링 제거)
        Page<MeetsEntity>  meetEntities = meetRepo.findAll(newPageable);

        return meetEntities.map(entity->{
            MeetsDto meetDto = MeetsDto.toDto(entity);
            //로그인한 유저가 지원한 게시글 Id 목록에 포함되어 있으면 해당 객체의 Apply 값을 true
            meetDto.setApply(applyMeetIds.contains(entity.getId()));
            //로그인한 유저의 Id와 게시글 작성자의 Id가 일치하면 해당 객체의 Writer 값을 true
            meetDto.setWriter(loginUser != null && meetDto.getUser().getId() == loginUser.getId());
            meetDto.setTotalApply(meetApplyRepo.countByMeetId(entity.getId()));
            return meetDto;
            });
            //sorted((x,y)->Long.compare(y.getId(), x.getId()))  Id값으로 내림차순 정렬
    }

    //<<상세 게시글 조회>>
    public MeetsDto getMeet(long id, UsersDto loginUser) {
        MeetsEntity meetEntity = meetRepo.findById(id).orElseGet(()->null);
        MeetsDto meetDto = MeetsDto.toDto(meetEntity);
        meetDto.setApproved(meetApplyRepo.countByMeetIdAndStatus(id,ApprovalStatus.APPROVED));

//        if (loginUser == null) {
//            throw new IllegalStateException("로그인이 필요합니다.");
//            //ResponseEntity로 변경시
//            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
//        }
        if(meetEntity != null){
            //세션에 저장된 유저 아이디(로그인한 사용자 아이디)가 지원한 게시물인지아닌지
            meetDto.setApply(loginUser != null && meetApplyRepo.existsByUserIdAndMeetId(loginUser.getId(),id));
            return  meetDto;
        }
        return  meetDto;
    }

    //<<게시글 삭제>>
    @Transactional
    public void deleteMeet(long id) {
        //연관된 자식필드를 먼저 삭제해야한다
        //연관된 자식필드 1. 댓글 2. 지원  3. 카테고리(는 ManyToMany로 설정했기 때문에 JPA에서 자동으로 연관 중간 테이블을 삭제해준다. )
        meetCommentsRepo.deleteAllByMeetId(id);
        meetApplyRepo.deleteAllByMeetId(id);

        meetRepo.deleteById(id);
    }

    //<<게시글 수정>>
    public MeetsDto updateMeet(MeetsDto dto) {
        MeetsEntity meetEntity = meetRepo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));
        
        //수정한 goal이 더 적을 수는 없게 프론트에서 처리함
        //수정한 goal이 더 많을경우 :
        if(dto.getGoal() > meetEntity.getGoal()){
            meetEntity.setActive(true);
        }
        meetEntity.setTitle(dto.getTitle());
        meetEntity.setContent(dto.getContent());
        meetEntity.setGoal(dto.getGoal());
        meetEntity.setMeetAt(dto.getMeetAt());
        meetEntity.setUpdatedAt(LocalDateTime.now());
        return MeetsDto.toDto(meetRepo.save(meetEntity));
    }
    //<<지원 생성>>
    public MeetApplyDto createApply(MeetApplyDto dto) {
        MeetApplyEntity meetApplyEntity = dto.toEntity();
        UsersEntity user = userRepo.findById(dto.getUser().getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        MeetsEntity meet = meetRepo.findById(dto.getMeet().getId()).orElseThrow(() -> new EntityNotFoundException("Meet not found"));
        meetApplyEntity.setUser(user);
        meetApplyEntity.setMeet(meet);
        meetApplyEntity = meetApplyRepo.save(meetApplyEntity);
        //int currentCount = meetApplyRepo.countByMeetIdAndStatus(meet.getId(),ApprovalStatus.APPROVED,ApprovalStatus.PENDING);
        return MeetApplyDto.toDto(meetApplyEntity);
    }

    public List<MeetApplyDto> getApplyAll(long id) {
        //해당 게시글 id로 지원 조회 , 그리고 지원생성 순서로 가져온다.
        List<MeetApplyEntity> meetApplyEntities = meetApplyRepo.findAllByMeetIdOrderByIdAsc(id);
        //List<MeetApplyEntity> mApplyEntities = meetApplyRepo.findAllByMeetId(id);
        return meetApplyEntities.stream().map(entity -> MeetApplyDto.toDto(entity)).collect(Collectors.toList());
    }

    public List<MeetApplyDto> getApplyAllByAdmin(long id) {
        //해당 게시글 id로 지원 조회 , 그리고 지원생성 순서로 가져온다. 
        List<MeetApplyEntity> meetApplyEntities = meetApplyRepo.findAllByMeetIdOrderByIdAsc(id);
        return meetApplyEntities.stream().map(entity -> MeetApplyDto.toDto(entity)).collect(Collectors.toList());
    }

    public MeetApplyDto acceptApplyUser(long meetId, UsersDto dto) {
        MeetApplyEntity meetApplyEntity = meetApplyRepo.findByMeetIdAndUserId(meetId,dto.getId()).orElseThrow(() -> new EntityNotFoundException("신청이 존재하지 않아요"));
        MeetsEntity meetsEntity = meetRepo.findById(meetId).orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다"));

        //1. 승인 전 체크 - 해당 게시글의 모집정원과  승인받은사람 수를 비교해야한다. (승인 전 체크가 꼭 필요한건 아니지만)
        // 해당 게시글 id 로 meetApplyRepo에서 찾고, status가 승인인 경우의 갯수를 반환해야한다. 
        int approvedCount = meetApplyRepo.countByMeetIdAndStatus(meetId,ApprovalStatus.APPROVED);
        if( meetsEntity.getGoal() <= approvedCount ) {
            meetsEntity.setActive(false);
            meetRepo.save(meetsEntity);
            throw new IllegalArgumentException("정원이 가득 찼습니다.");
        }

        //해당 지원 테이블의 ApprovalStatus 값을 승인상태로.
        meetApplyEntity.setStatus(ApprovalStatus.APPROVED);
        //해당 지원 테이블의 승인 시각을 세팅
        meetApplyEntity.setApprovedAt(LocalDateTime.now());
        
        //2. 승인 후 정원이 가득 찼을경우 Active 값 변경 
        if( meetsEntity.getGoal() == approvedCount +1 ) {
            meetsEntity.setActive(false);
            
        }
        //반환할 meetApplyDto에 최신화된 meets값 저장
        meetApplyEntity.setMeet(meetRepo.save(meetsEntity));
        return MeetApplyDto.toDto(meetApplyRepo.save(meetApplyEntity));
    }

    public MeetApplyDto rejectApplyUser(long meetId, UsersDto dto) {
        MeetApplyEntity meetApplyEntity = meetApplyRepo.findByMeetIdAndUserId(meetId,dto.getId()).orElseThrow(() -> new EntityNotFoundException("apply not found"));
        MeetsEntity meetsEntity = meetRepo.findById(meetId).orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다"));
        //해당 지원 테이블의 ApprovalStatus 값을 거절상태로.
        meetApplyEntity.setStatus(ApprovalStatus.REJECTED);
        //반환할 meetApplyDto에 최신화된 meets값 저장
        meetApplyEntity.setMeet(meetRepo.save(meetsEntity));
        return MeetApplyDto.toDto(meetApplyRepo.save(meetApplyEntity));
    }

    public MeetsDto closeApply(long id) {
        MeetsEntity meetEntity = meetRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Meet not found"));
        if (!meetEntity.isActive()) {
            throw new IllegalStateException("이미 마감된 모집입니다.");
        }
        meetEntity.setActive(false);
        return MeetsDto.toDto(meetRepo.save(meetEntity));
    }

    public List<MeetsDto> getClosingSoonMeets(int limit,UsersDto user) {
        //방법1 (네이티브 쿼리 미사용) - 전체 모임 조회
        List<MeetsEntity> meetsEntities  = meetRepo.findAllByIsActiveOrderByMeetAtAsc(true);
        return meetsEntities.stream().limit(limit).map(meentEntity -> {
            MeetsDto meetDto = MeetsDto.toDto(meentEntity);
            meetDto.setTotalApply(meetApplyRepo.countByMeetId(meentEntity.getId()));
            return meetDto;
        }).collect(Collectors.toList());
    }

    public List<MeetsDto> getHotMeets(int limit, UsersDto loginUser, Long TeamId) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return null;
        List<MeetsEntity> meetsEntities  = meetRepo.findByTeamIdOrderByCommentsAndApplies(limit, TeamId);
        return meetsEntities.stream().map(meentEntity -> MeetsDto.toDto(meentEntity)).collect(Collectors.toList());
    }

    
    //검색 기능이 포함된 전체 글 조회
    public Page<MeetsDto> meetAllWithSearch(Pageable pageable, String search) {
        Pageable newPageable = PageRequest.of(pageable.getPageNumber()-1, pageable.getPageSize(), pageable.getSort());
        Page<MeetsEntity> meetEntities;
        
        if (search != null && !search.trim().isEmpty()) {
            meetEntities = meetRepo.findByTitleContainingOrUser_NameContainingOrTeam_NameContaining(
                search.trim(), search.trim(), search.trim(), newPageable);
        } else {
            meetEntities = meetRepo.findAll(newPageable);
        }
        
        return meetEntities.map(entity -> MeetsDto.toDto(entity));
    }
    //상세 글 조회
    public MeetsDto getMeet(long id) {
        MeetsEntity meetsEntity = meetRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않아요"));
        return MeetsDto.toDto(meetsEntity); 
    }

    @Transactional
    public MeetsDto cancleApply(long meetId, long userId) {
        MeetApplyEntity meetApplyEntity = meetApplyRepo.findByMeetIdAndUserId(meetId,userId).orElseThrow(() -> new EntityNotFoundException("apply not found"));
        MeetsEntity meetsEntity =  meetRepo.findById(meetId).orElseThrow(() -> new EntityNotFoundException("meet not found"));
        //참가자가 취소한 경우
        if(meetApplyEntity.getStatus() == ApprovalStatus.APPROVED ){
           meetsEntity.setActive(true);
        } 
        
        meetApplyRepo.delete(meetApplyEntity);
        return MeetsDto.toDto(meetsEntity);
    }

    public Map<String,List<Integer>> getAllBbsCount() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //현재 시각
        LocalDate currenDate= LocalDate.now();

        List<Object[]> allCount = meetRepo.getAllBbsCountBySeven();
        List<Integer> feedCount = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            feedCount.add(0);
        }
        List<Integer> placeCount = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            placeCount.add(0);
        }
        List<Integer> meetCount = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            meetCount.add(0);
        }
        for(Object[] obj : allCount){
            String type = (String) obj[0]; // type
            Date day = (Date) obj[1]; // day
            String dayString = sdf.format(day);
            //현재날짜 - 받아온 date의 날짜. 
            long diff = currenDate.toEpochDay() - LocalDate.parse(dayString).toEpochDay();
            int index = 7 - (int)diff;
            int count = ((BigDecimal)obj[2]).intValue(); // 오라클에서는 COUNT(*) 결과를 BigDecimal으로 반환
            if("feed".equals(type)){
                feedCount.set(index, count);
            }
            if("place".equals(type)){
                placeCount.set(index, count);
            }
            if("meet".equals(type)){
                meetCount.set(index, count);
            }
        }
        Map<String,List<Integer>> result = new HashMap<>();
        result.put("feedCount", feedCount);
        result.put("placeCount", placeCount);
        result.put("meetCount", meetCount);
        return result;
    }

}
