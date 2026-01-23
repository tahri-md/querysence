package com.example.querysence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.Project;
import com.example.querysence.model.User;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByOwner(User owner);
    
    List<Project> findByOwnerId(Long ownerId);
    
    Optional<Project> findByIdAndOwner(Long id, User owner);
    
    @Query("SELECT p FROM Project p WHERE p.owner = :owner ORDER BY p.updatedAt DESC")
    List<Project> findRecentProjectsByOwner(@Param("owner") User owner);
}