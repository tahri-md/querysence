package com.example.querysence.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanDto {

    private Long id;
    private String source;          // STATIC_HEURISTIC or LIVE_EXPLAIN
    private String planText;
    private Double estimatedCost;
    private Long actualRows;        // null when source = STATIC_HEURISTIC
    private Double actualTimeMs;    // null when source = STATIC_HEURISTIC
    private List<String> usedIndexes;
    private List<String> fullTableScans;
}
