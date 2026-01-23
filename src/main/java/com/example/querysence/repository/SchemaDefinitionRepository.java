package com.example.querysence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.Project;
import com.example.querysence.model.SchemaDefinition;

@Repository
public interface SchemaDefinitionRepository extends JpaRepository<SchemaDefinition, Long> {

    List<SchemaDefinition> findByProject(Project project);

    List<SchemaDefinition> findByProjectId(Long projectId);

    Optional<SchemaDefinition> findByProjectIdAndName(Long projectId, String name);

    List<SchemaDefinition> findByDialect(String dialect);

      @Query("SELECT s FROM SchemaDefinition s " +
           "LEFT JOIN FETCH s.tables t " +
           "LEFT JOIN FETCH t.columns " +
           "LEFT JOIN FETCH t.indexes " +
           "WHERE s.id = :id")
    Optional<SchemaDefinition> findByIdWithFullDetails(Long id);
}
