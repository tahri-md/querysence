package com.example.querysence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "column_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableDefinition table;
    
    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;
    
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;
    
    @Column(name = "is_nullable")
    @Builder.Default
    private Boolean isNullable = true;
    
    @Column(name = "is_primary_key")
    @Builder.Default
    private Boolean isPrimaryKey = false;
    
    @Column(name = "is_foreign_key")
    @Builder.Default
    private Boolean isForeignKey = false;
    
    @Column(name = "references_table", length = 100)
    private String referencesTable;
    
    @Column(name = "references_column", length = 100)
    private String referencesColumn;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}