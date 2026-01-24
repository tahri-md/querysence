package com.example.querysence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.Project;
import com.example.querysence.model.QueryHistory;
import com.example.querysence.model.User;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    
    Page<QueryHistory> findByUser(User user, Pageable pageable);
    
    Page<QueryHistory> findByUserAndProject(User user, Project project, Pageable pageable);
    
    Page<QueryHistory> findByUserAndAnalyzedAtBetween(User user, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    @Query("SELECT qh FROM QueryHistory qh " +
           "LEFT JOIN FETCH qh.indexSuggestions " +
           "LEFT JOIN FETCH qh.securityFindings " +
           "WHERE qh.id = :id")
    Optional<QueryHistory> findByIdWithDetails(Long id);
    
    Optional<QueryHistory> findByQueryHash(String queryHash);
    
    @Query("SELECT AVG(qh.complexityScore) FROM QueryHistory qh WHERE qh.user = :user")
    Double getAverageComplexityByUser(User user);
    
    @Query("SELECT COUNT(qh) FROM QueryHistory qh WHERE qh.user = :user")
    Long countByUser(User user);
    
    @Query("SELECT qh.queryType, COUNT(qh) FROM QueryHistory qh " +
           "WHERE qh.user = :user GROUP BY qh.queryType")
    List<Object[]> countByUserGroupByQueryType(User user);
    
    @Query("SELECT DATE(qh.analyzedAt), COUNT(qh) FROM QueryHistory qh " +
           "WHERE qh.user = :user AND qh.analyzedAt >= :startDate " +
           "GROUP BY DATE(qh.analyzedAt) ORDER BY DATE(qh.analyzedAt)")
    List<Object[]> getQueryTrendByUser(User user, LocalDateTime startDate);
    
    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user " +
           "AND qh.executionTimeMs > (SELECT AVG(qh2.executionTimeMs) * 2 FROM QueryHistory qh2 " +
           "WHERE qh2.queryHash = qh.queryHash)")
    List<QueryHistory> findSlowQueriesByUser(User user);
}