package com.example.querysence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.TableDefinition;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface TableDefinitionRepository extends JpaRepository<TableDefinition, Long> {

    List<TableDefinition> findBySchema(SchemaDefinition schema);

    List<TableDefinition> findBySchemaId(Long schemaId);

    Optional<TableDefinition> findBySchemaIdAndTableName(Long schemaId, String tableName);

    @Query("SELECT t FROM TableDefinition t WHERE t.schema.id = :schemaId AND t.estimatedRows > :minRows")
    List<TableDefinition> findLargeTablesBySchema(@Param("schemaId") Long schemaId,
            @Param("minRows") Long minRows);

    
    
    Optional<TableDefinition> findBySchemaAndTableName(SchemaDefinition schema, String tableName);
    
    @Query("SELECT t FROM TableDefinition t " +
           "LEFT JOIN FETCH t.columns " +
           "LEFT JOIN FETCH t.indexes " +
           "WHERE t.id = :id")
    Optional<TableDefinition> findByIdWithDetails(Long id);
    
    boolean existsBySchemaAndTableName(SchemaDefinition schema, String tableName);
}