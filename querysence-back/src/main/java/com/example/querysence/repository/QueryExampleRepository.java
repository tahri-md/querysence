package com.example.querysence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.querysence.model.QueryExample;

public interface QueryExampleRepository
                extends JpaRepository<QueryExample, Long> {

        List<QueryExample> findBySchemaIdOrderByCreatedAtDesc(
                        Long schemaId);

        List<QueryExample> findByUserIdAndVerifiedTrueOrderByAccessCountDesc(
                        Long userId);

        @Query("""
                        SELECT q
                        FROM QueryExample q
                        WHERE q.confidenceScore > :threshold
                        ORDER BY q.accessCount DESC
                        """)
        List<QueryExample> findReliableQueries(
                        Double threshold);

        @Query("""
                        SELECT q
                        FROM QueryExample q
                        WHERE q.createdAt > :since
                        ORDER BY q.lastAccessedAt DESC
                        """)
        List<QueryExample> findRecentQueries(
                        LocalDateTime since);

        Long deleteByCreatedAtBefore(
                        LocalDateTime date);
}