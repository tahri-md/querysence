package com.example.querysence.model;

import java.time.LocalDateTime;

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
@Table(name = "optimization_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_history_id", nullable = false)
    private QueryHistory queryHistory;
    
    @Column(name = "original_query", nullable = false, columnDefinition = "TEXT")
    private String originalQuery;
    
    @Column(name = "optimized_query", columnDefinition = "TEXT")
    private String optimizedQuery;
    
    @Column(name = "optimization_type", nullable = false, length = 50)
    private String optimizationType;
    
    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;
    
    @Column(name = "applied")
    @Builder.Default
    private Boolean applied = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
