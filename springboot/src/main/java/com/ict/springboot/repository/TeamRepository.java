package com.ict.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ict.springboot.entity.TeamEntity;

@Repository
public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    public Optional<TeamEntity> findByName(String name);
    public Optional<TeamEntity> findByIdKey(String idKey);
  
}