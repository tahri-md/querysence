package com.example.querysence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.IndexDefinition;

import com.example.querysence.model.TableDefinition;

@Repository
public interface IndexDefinitionRepository extends JpaRepository<IndexDefinition, Long> {

    List<IndexDefinition> findByTable(TableDefinition table);

    List<IndexDefinition> findByTableId(Long tableId);

    Optional<IndexDefinition> findByTableIdAndIndexName(Long tableId, String indexName);

    List<IndexDefinition> findByIsUniqueTrue();
}