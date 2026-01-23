package com.example.querysence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.ColumnDefinition;

import com.example.querysence.model.TableDefinition;

@Repository
public interface ColumnDefinitionRepository extends JpaRepository<ColumnDefinition, Long> {

    List<ColumnDefinition> findByTable(TableDefinition table);

    List<ColumnDefinition> findByTableId(Long tableId);

    List<ColumnDefinition> findByIsPrimaryKeyTrue();

    List<ColumnDefinition> findByIsForeignKeyTrue();

    @Query("SELECT c FROM ColumnDefinition c WHERE c.table.id = :tableId AND c.isPrimaryKey = true")
    List<ColumnDefinition> findPrimaryKeysByTableId(@Param("tableId") Long tableId);
}