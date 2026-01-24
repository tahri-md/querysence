package com.example.querysence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "query_history", indexes = {
    @Index(name = "idx_query_history_user", columnList = "user_id"),
    @Index(name = "idx_query_history_project", columnList = "project_id"),
    @Index(name = "idx_query_history_hash", columnList = "query_hash")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Column(name = "query_type", nullable = false, length = 20)
    private String queryType;

    @Column(name = "complexity_score")
    private Integer complexityScore;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @OneToMany(mappedBy = "queryHistory", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private Set<IndexSuggestion> indexSuggestions = new HashSet<>();

    @OneToMany(mappedBy = "queryHistory", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SecurityFinding> securityFindings = new ArrayList<>();

    @CreatedDate
    @Column(name = "analyzed_at", updatable = false)
    private LocalDateTime analyzedAt;
}
