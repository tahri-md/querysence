package com.example.querysence.repository;

import com.example.querysence.model.ProjectInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, Long> {
    List<ProjectInvite> findByProjectId(Long projectId);
    Optional<ProjectInvite> findByInviteCode(String inviteCode);
    List<ProjectInvite> findByEmailAndIsUsedFalse(String email);
}
