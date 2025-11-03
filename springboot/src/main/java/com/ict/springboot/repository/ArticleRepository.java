package com.ict.springboot.repository;

import com.ict.springboot.entity.ArticleEntity;
import com.ict.springboot.entity.SimulationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    List<ArticleEntity> findBySimulation(SimulationEntity simulation);
}