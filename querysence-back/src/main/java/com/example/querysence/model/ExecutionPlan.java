package com.example.querysence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "execution_plans")
@Data
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
