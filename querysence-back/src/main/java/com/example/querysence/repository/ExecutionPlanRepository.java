package com.example.querysence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.ExecutionPlan;

@Repository
public interface ExecutionPlanRepository extends JpaRepository<ExecutionPlan, Long> {

    Optional<ExecutionPlan> findByQueryHistoryId(Long queryHistoryId);
}
