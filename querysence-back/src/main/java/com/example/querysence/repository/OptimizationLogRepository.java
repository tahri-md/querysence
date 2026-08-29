package com.example.querysence.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.querysence.model.OptimizationLog;
import com.example.querysence.model.QueryHistory;

public interface OptimizationLogRepository extends JpaRepository<OptimizationLog, Long> {

    List<OptimizationLog> findByQueryHistory(QueryHistory queryHistory);

    List<OptimizationLog> findByQueryHistoryId(Long queryHistoryId);

    List<OptimizationLog> findByOptimizationType(String optimizationType);

    List<OptimizationLog> findByAppliedTrue();

    @Query("SELECT o FROM OptimizationLog o WHERE o.queryHistory.user.id = :userId " +
            "ORDER BY o.createdAt DESC")
    List<OptimizationLog> findByUserId(@Param("userId") Long userId, Pageable pageable);
}