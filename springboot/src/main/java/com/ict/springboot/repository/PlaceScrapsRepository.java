package com.ict.springboot.repository;

import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.PlaceScrapsEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface PlaceScrapsRepository extends JpaRepository<PlaceScrapsEntity, Long> {

	boolean existsByUserIdAndPlaceId(Long userId, Long placeId);

	PlaceScrapsEntity findByUserIdAndPlaceId(Long userId, Long placeId);

	int countByPlaceId(Long placeId);

	List<PlaceScrapsEntity> findByUserId(Long userId);

}