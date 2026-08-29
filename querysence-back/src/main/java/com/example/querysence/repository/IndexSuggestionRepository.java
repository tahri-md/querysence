package com.example.querysence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.querysence.model.IndexSuggestion;
import com.example.querysence.model.QueryHistory;

public interface IndexSuggestionRepository extends JpaRepository<IndexSuggestion, Long> {

    List<IndexSuggestion> findByQueryHistory(QueryHistory queryHistory);

    List<IndexSuggestion> findByQueryHistoryId(Long queryHistoryId);

    List<IndexSuggestion> findByTableName(String tableName);

    List<IndexSuggestion> findByImpactScore(String impactScore);

    @Query("SELECT i FROM IndexSuggestion i WHERE i.queryHistory.user.id = :userId " +
            "AND i.impactScore = 'HIGH' ORDER BY i.createdAt DESC")
    List<IndexSuggestion> findHighImpactSuggestionsByUser(@Param("userId") Long userId);
}