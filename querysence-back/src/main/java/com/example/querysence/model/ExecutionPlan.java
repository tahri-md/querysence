package com.example.querysence.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "execution_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_history_id", nullable = false)
    private QueryHistory queryHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "db_connection_id")
    private DbConnection dbConnection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlanSource source = PlanSource.STATIC_HEURISTIC;

    @Column(name = "plan_text", columnDefinition = "TEXT")
    private String planText;

    @Column(name = "plan_json", columnDefinition = "TEXT")
    private String planJson;

    @Column(name = "estimated_cost")
    private Double estimatedCost;

    @Column(name = "actual_rows")
    private Long actualRows;

    @Column(name = "actual_time_ms")
    private Double actualTimeMs;

    @Column(name = "used_indexes", columnDefinition = "TEXT[]")
    private List<String> usedIndexes;

    @Column(name = "full_table_scans", columnDefinition = "TEXT[]")
    private List<String> fullTableScans;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
