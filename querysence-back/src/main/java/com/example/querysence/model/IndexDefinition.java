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
@Table(name = "index_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableDefinition table;
    
    @Column(name = "index_name", nullable = false, length = 100)
    private String indexName;
    
    @Column(nullable = false, columnDefinition = "TEXT[]")
    private List<String> columns;
    
    @Column(name = "is_unique")
    @Builder.Default
    private Boolean isUnique = false;
    
    @Column(name = "index_type", length = 20)
    @Builder.Default
    private String indexType = "BTREE";
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}