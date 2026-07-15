package com.example.querysence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.DbConnection;
import com.example.querysence.model.SchemaSyncLog;

@Repository
public interface SchemaSyncLogRepository extends JpaRepository<SchemaSyncLog, Long> {

    List<SchemaSyncLog> findByDbConnection(DbConnection dbConnection);

    List<SchemaSyncLog> findByDbConnectionId(Long dbConnectionId);

    @Query("SELECT s FROM SchemaSyncLog s WHERE s.dbConnection.id = :dbConnectionId " +
            "ORDER BY s.startedAt DESC")
    List<SchemaSyncLog> findMostRecentByDbConnectionId(@Param("dbConnectionId") Long dbConnectionId);
}
