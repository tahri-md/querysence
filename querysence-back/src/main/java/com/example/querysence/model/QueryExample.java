package com.example.querysence.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "query_examples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class QueryExample {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id")
    private SchemaDefinition schema;
    @Column(columnDefinition = "TEXT")
    private String nlQuery;
    @Column(columnDefinition = "TEXT")
    private String sqlOutput;
    @Transient
    private float[] embedding;
    private String queryType;
    private Double confidenceScore;
    private Integer tokenCount;
    private Long executionTimeMs;
    private Boolean verified;
    @Column(name = "pinecone_vector_id")
    private String vectorId;
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private Integer accessCount;
    private LocalDateTime lastAccessedAt;

}
