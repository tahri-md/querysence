package com.example.querysence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.querysence.model.ExecutionPlan;

public interface ExecutionPlanRepository extends JpaRepository<ExecutionPlan, Long> {

    Optional<ExecutionPlan> findByQueryHistoryId(Long queryHistoryId);
}
