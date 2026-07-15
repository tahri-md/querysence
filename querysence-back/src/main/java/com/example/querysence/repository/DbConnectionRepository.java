package com.example.querysence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.DbConnection;
import com.example.querysence.model.Project;

@Repository
public interface DbConnectionRepository extends JpaRepository<DbConnection, Long> {

    List<DbConnection> findByProject(Project project);

    List<DbConnection> findByProjectId(Long projectId);

    Optional<DbConnection> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByNameAndProject(String name, Project project);
}
