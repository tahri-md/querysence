package com.example.querysence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "schema_sync_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "db_connection_id", nullable = false)
    private DbConnection dbConnection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SyncStatus status = SyncStatus.RUNNING;

    @Column(name = "tables_discovered")
    private Integer tablesDiscovered;

    @Column(name = "columns_discovered")
    private Integer columnsDiscovered;

    @Column(name = "indexes_discovered")
    private Integer indexesDiscovered;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
