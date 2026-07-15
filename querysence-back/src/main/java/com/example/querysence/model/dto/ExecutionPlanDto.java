package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
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
