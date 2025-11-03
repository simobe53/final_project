package com.ict.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity,Long> {

    boolean existsByAccount(String account);

    Optional<UsersEntity> findByAccount(String account);

    Optional<UsersEntity> findByIdAndAccountAndPassword(Long id, String account, String password);
    
    // 전체 유저 조회 (생성일 기준 내림차순)
    Page<UsersEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 이름 또는 팀명으로 검색 (생성일 기준 내림차순)
    @Query("SELECT u FROM UsersEntity u WHERE u.name LIKE %:keyword% OR u.team.name LIKE %:keyword% ORDER BY u.createdAt DESC")
    Page<UsersEntity> findByNameOrAddressContainingOrderByCreatedAtDesc(@Param("keyword") String keyword, Pageable pageable);


    @Query("SELECT u FROM UsersEntity u WHERE " +
            "(:account IS NULL OR u.account LIKE %:account%) AND " +
            "(:name IS NULL OR u.name LIKE %:name%) AND " +
            "(:team IS NULL OR u.team IS NULL OR u.team.name LIKE %:team%) AND " +
            "(:role IS NULL OR u.role LIKE %:role%) AND " +
            "(:gender IS NULL OR u.gender LIKE %:gender%)")
    List<UsersEntity> searchByParams(
        @Param("account") Object account,
        @Param("name") Object name,
        @Param("gender") Object gender,
        @Param("team") Object team,
        @Param("role") Object role
    );

    //아이디와 비밀번호가 맞는지 비교하기 위한 쿼리 메소드
	Optional<UsersEntity> findByAccountAndPassword(String account, String password);

    UsersEntity findByAccountAndPasswordAndRole(String account, String password, String string);    
  

}