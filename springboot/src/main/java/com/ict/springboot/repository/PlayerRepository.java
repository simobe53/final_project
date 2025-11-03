package com.ict.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ict.springboot.entity.PlayerEntity;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    PlayerEntity findBypNo(Long pNo);
    // 팀별 선수 조회
    List<PlayerEntity> findByTeamIdKey(String idKey);
    // 선수 이름으로 조회 (여러 결과 가능)
    List<PlayerEntity> findByPlayerName(String playerName);
    // 선수 이름과 팀으로 조회 (고유 결과)
    PlayerEntity findByPlayerNameAndTeamIdKey(String playerName, String teamIdKey);
    
}
