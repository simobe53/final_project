package com.ict.springboot.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

private final UsersRepository usersRepository;
	

	//아이디와 비밀번호가 맞는지 비교해보고 맞으면 해당 유저 정보를 반환하고 없으면 null 반환
    public UsersDto isUser(UsersDto user) {

    	//로그인하는 계정이 있는지 확인
		Optional<UsersEntity> userOptional= usersRepository.findByAccountAndPassword(user.getAccount(), user.getPassword());
		//비밀번호를 제외한 나머지 값을 세션에 저장
		UsersDto sessionDto = (userOptional.map(UsersDto::sessionDto).orElse(null));
		
		return sessionDto;
	}

	public void saveUser(UsersDto dto) {
		UsersEntity user = usersRepository.findByAccount(dto.getAccount()).orElseGet(()->null);
		if (user == null) {
			usersRepository.save(dto.toEntity());
		}
	}

    public UsersDto findLoginUser(UsersDto loginUser) {
		if(loginUser != null) {
			UsersEntity user = usersRepository.findByAccount(loginUser.getAccount()).orElseGet(()->null);
	        return UsersDto.toDto(user);
		}
    	return null;
    }

	public boolean isAdmin(UsersDto user) {
		if (user == null) return false;	// 로그인 되어있지 않은 유저

		UsersDto loginUser = UsersDto.toDto(usersRepository.findByAccount(user.getAccount()).orElseGet(()->null));
		if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return false; // ADMIN이 아닌 유저

		return true;
	}

    public UsersDto adminLogin(String account, String password) {
        UsersEntity usersEntity = usersRepository.findByAccountAndPasswordAndRole(account, password, "ADMIN");
		return UsersDto.toDto(usersEntity);
    }
}
