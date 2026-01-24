package com.example.querysence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_findings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityFinding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_history_id", nullable = false)
    private QueryHistory queryHistory;
    
    @Column(name = "finding_type", nullable = false, length = 50)
    private String findingType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "line_number")
    private Integer lineNumber;
    
    @Column(columnDefinition = "TEXT")
    private String recommendation;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}