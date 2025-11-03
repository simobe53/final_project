package com.ict.springboot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository userRepo;
    private final MailService mailService;

    

    /* 전체 조회 */
    public List<UsersDto> getAll() {
        List<UsersEntity> usersEntities = userRepo.findAll();
        return usersEntities.stream().map(entity -> UsersDto.toDto(entity)).collect(Collectors.toList());
    }

    /* 검색 조회 (**안해도 무방한데 관리자 페이지에 검색을 넣으면 좋을 것 같아서 넣어봤습니다.) */
    public List<UsersDto> searchByParams(Map<String, String> params) {
        List<UsersEntity> usersEntities = userRepo.searchByParams(
            params.get("account"), 
            params.get("name"), 
            params.get("gender"), 
            params.get("location"), 
            params.get("role")
        );
        return usersEntities.stream().map(entity -> UsersDto.toDto(entity)).collect(Collectors.toList());
    }

    /* 상세 조회 */
    public UsersDto getByAccount(String account) {
        Optional<UsersEntity> usersEntity = userRepo.findByAccount(account);
        return UsersDto.toDto(usersEntity.orElseGet(()->null));
    }

    /* 생성 */
    public UsersDto create(UsersDto dto) {
        boolean isDuplicated = userRepo.existsByAccount(dto.getAccount()); // 기존 테이블에 저장되어있는지 체크 (유니크한 키로 작성)
        if (isDuplicated) return null; // 유니크 중복의 경우 저장안함 (아래로 내려갈시 에러 반환)
        UsersEntity usersEntity = userRepo.save(dto.toEntity());     // 저장 후 반환되는 객체를 받아
        return UsersDto.toDto(usersEntity);     // dto 상태로 변환하여 리턴
    }

    /* 수정 */
    public UsersDto update(String account, UsersDto newUser, UsersDto loginUser) {
        UsersDto user = UsersDto.toDto(userRepo.findByAccount(account).orElseGet(()-> null));
        if (user == null) return null;  // 수정 불가능
        if (loginUser == null) return null;  // 로그인 필요
        if (!loginUser.getRole().equals("ADMIN") && !loginUser.getAccount().equals(user.getAccount())) return null;  // 수정 불가능
        // 값이 있는 경우에만 수정한다. (null로 덮어쓰지 않는다.)
        if (newUser.getName() != null) user.setName(newUser.getName());
        if (newUser.getGender() != null) user.setGender(newUser.getGender());
        if (newUser.getPassword() != null) user.setPassword(newUser.getPassword());
        if (newUser.getProfileImage() != null) user.setProfileImage(newUser.getProfileImage());
        if (newUser.getEmail() != null) user.setEmail(newUser.getEmail());
        if (newUser.getRole() != null) user.setRole(newUser.getRole());
        if (newUser.getTeam() != null) user.setTeam(newUser.getTeam());
        UsersEntity usersEntity = userRepo.save(user.toEntity());
        return UsersDto.toDto(usersEntity);
    }

    /* 삭제 */
    public UsersDto delete(String account) throws Exception {
        UsersEntity user = userRepo.findByAccount(account).orElseGet(()-> null);
        if (user != null) {
            try {
                userRepo.deleteById(user.getId());
                return UsersDto.toDto(user);
            } catch (Exception e) {
                throw new Exception("데이터 삭제에 문제가 생겼습니다.");
            }
        }
        return null;
    }
  
   
    /* 중복 조회 */
    public boolean checkExists(String account) {
        return userRepo.existsByAccount(account);
    }
    

    public UsersDto changePassword(Long id, Map<String, String> passwords, UsersDto loginUser) {
        if (loginUser == null) return null;
        String account = loginUser.getAccount();
        String orgPassword = passwords.get("orgPassword");
        UsersEntity targetUser = userRepo.findByIdAndAccountAndPassword(id, account, orgPassword).orElseGet(()->null);
        // 일치하는 유저가 없을 경우
        if (targetUser == null) return null;

        String password = passwords.get("password");
        targetUser.setPassword(password);   // 비밀번호 변경 후 저장
        UsersEntity saved = userRepo.save(targetUser);
        return UsersDto.toDto(saved);
    }

    // 관리자용 유저 조회 (페이지네이션 정보 포함)
    public Page<UsersDto> getUsersForAdminWithPagination(int page, int size, UsersDto loginUser) {
        if (loginUser == null) return null;
        UsersEntity user = userRepo.findByAccount(loginUser.getAccount()).orElseGet(()->null);
        if (user == null) return null;

        Pageable pageable = PageRequest.of(page, size);
        Page<UsersEntity> usersPage = userRepo.findAllByOrderByCreatedAtDesc(pageable);

        Page<UsersDto> usersDtoPage = usersPage.map(entity -> {
            UsersDto dto = UsersDto.toDto(entity);
            return dto;
        });

        return usersDtoPage;
    }
    
    // 관리자용 유저 검색 (페이지네이션 정보 포함)
    public Page<UsersDto> searchUsersForAdminWithPagination(String keyword, int page, int size, UsersDto loginUser) {
        if (loginUser == null) return null;
        UsersEntity user = userRepo.findByAccount(loginUser.getAccount()).orElseGet(()->null);
        if (user == null) return null;

        Pageable pageable = PageRequest.of(page, size);
        Page<UsersEntity> usersPage = userRepo.findByNameOrAddressContainingOrderByCreatedAtDesc(keyword, pageable);

        Page<UsersDto> usersDtoPage = usersPage.map(entity -> {
            UsersDto dto = UsersDto.toDto(entity);
            return dto;
        });

        return usersDtoPage;
    }


    /**
     * 비밀번호 찾기 - 아이디, 이메일로 사용자 확인 후 8자리 임시 비밀번호 생성
     */
    public Map<String, Object> resetPassword(String account, String email) {
        Map<String, Object> result = new HashMap<>();

        // 1. 사용자 확인 (아이디, 이메일이 일치하는지)
        Optional<UsersEntity> userOptional = userRepo.findByAccount(account);

        if (!userOptional.isPresent()) {
            result.put("success", false);
            result.put("message", "일치하는 사용자 정보가 없습니다.");
            return result;
        }

        UsersEntity user = userOptional.get();

        // 이메일이 일치하는지 확인
        if (!user.getEmail().equals(email)) {
            result.put("success", false);
            result.put("message", "일치하는 사용자 정보가 없습니다.");
            return result;
        }

        // 2. 8자리 숫자 임시 비밀번호 생성
        String tempPassword = String.format("%08d", (int)(Math.random() * 100000000));

        // 3. 비밀번호 변경
        user.setPassword(tempPassword);
        userRepo.save(user);

        // 4. 이메일로 임시 비밀번호 전송
        try {
            mailService.sendPasswordResetMail(email, tempPassword);
            result.put("success", true);
            result.put("message", "임시 비밀번호가 이메일로 전송되었습니다.");
        } catch (Exception e) {
            result.put("success", true); // 비밀번호는 변경되었으므로 success는 true
            result.put("message", "비밀번호는 변경되었으나 이메일 전송에 실패했습니다. 임시 비밀번호: " + tempPassword);
        }

        return result;
    }

    /**
     * 포인트 조회
     */
    public Long getPoint(Long userId) {
        UsersEntity user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getPoint() != null ? user.getPoint() : 0L;
    }

}
