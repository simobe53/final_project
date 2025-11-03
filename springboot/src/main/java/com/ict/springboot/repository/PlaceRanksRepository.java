package com.ict.springboot.repository;

import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.PlaceRanksEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface PlaceRanksRepository extends JpaRepository<PlaceRanksEntity, Long> {

    boolean existsByUserIdAndPlaceId(long id, Long placeId);

    List<PlaceRanksEntity> findByPlaceId(Long placeId);

	List<PlaceRanksEntity> findByUserId(Long userId);
    
    
}
