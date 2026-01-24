package com.example.querysence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.QueryHistory;
import com.example.querysence.model.SecurityFinding;

@Repository
public interface SecurityFindingRepository extends JpaRepository<SecurityFinding, Long> {

       List<SecurityFinding> findByQueryHistory(QueryHistory queryHistory);

       List<SecurityFinding> findByQueryHistoryId(Long queryHistoryId);

       List<SecurityFinding> findBySeverity(SecurityFinding.Severity severity);

       @Query("SELECT s FROM SecurityFinding s WHERE s.queryHistory.user.id = :userId " +
                     "AND s.severity IN ('CRITICAL', 'HIGH') ORDER BY s.createdAt DESC")
       List<SecurityFinding> findCriticalFindingsByUser(@Param("userId") Long userId);

       @Query("SELECT COUNT(s) FROM SecurityFinding s WHERE s.queryHistory.user.id = :userId " +
                     "AND s.severity = :severity")
       Long countBySeverityAndUserId(@Param("severity") SecurityFinding.Severity severity,
                     @Param("userId") Long userId);
}