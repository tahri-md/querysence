package com.example.querysence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_logs")
@Data
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