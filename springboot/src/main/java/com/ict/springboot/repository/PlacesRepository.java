    package com.ict.springboot.repository;

    import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.PlacesEntity;

@Repository
public interface PlacesRepository extends JpaRepository<PlacesEntity, Long>{

    // 로그인 유저 팀별로 가져오기
    List<PlacesEntity> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    
    // 작성자 아이디(id)로 가져오기
    List<PlacesEntity> findByUserId(Long userId);


    //중복 조회
    boolean existsByName(String name);

    // 전체 플레이스 조회 (생성일 내림차순)
    Page<PlacesEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 이름 또는 주소로 검색
    @Query("SELECT p FROM PlacesEntity p WHERE p.name LIKE %:keyword% OR p.address LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<PlacesEntity> findByNameOrAddressContainingOrderByCreatedAtDesc(@Param("keyword") String keyword, Pageable pageable);

    // 메인을 위한 핫한 플레이스 
    @Query(
        value = """
            WITH RankedPlaces AS (
                SELECT 
                    p.id,
                    p.team_id,
                    p.address,
                    p.created_at,
                    p.category,
                    p.user_id,
                    p.name,
                    COALESCE(SUM(pr.placeRank), 0) * 2 + COUNT(ps.id) AS score
                FROM places p
                LEFT JOIN place_ranks pr ON pr.placeId = p.id
                LEFT JOIN place_scrap ps ON ps.placeId = p.id
                WHERE p.team_id = :teamId and p.created_at BETWEEN SYSDATE - 7 AND SYSDATE
                GROUP BY p.id, p.team_id, p.address, p.created_at, p.category, p.user_id, p.name
                ORDER BY score DESC
                FETCH FIRST :limit ROWS ONLY
            )
            SELECT rp.*, p.image
            FROM RankedPlaces rp
            LEFT JOIN places p ON p.id = rp.id
        """, 
        nativeQuery = true
    )
    List<PlacesEntity> findByTeamIdOrderByRanksAndScraps(Long teamId, int limit);
}
