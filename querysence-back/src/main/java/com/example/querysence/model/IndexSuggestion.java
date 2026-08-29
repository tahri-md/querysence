package com.example.querysence.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "index_suggestions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexSuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_history_id", nullable = false)
    private QueryHistory queryHistory;
    
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;
    
    @Column(nullable = false, columnDefinition = "TEXT[]")
    private List<String> columns;
    
    @Column(name = "suggestion_type", nullable = false, length = 50)
    private String suggestionType;
    
    @Column(name = "impact_score", nullable = false, length = 10)
    private String impactScore;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
