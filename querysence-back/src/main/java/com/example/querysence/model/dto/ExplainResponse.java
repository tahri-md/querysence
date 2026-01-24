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
public class ExplainResponse {
    
    private String summary;
    private List<ClauseBreakdown> breakdown;
    private String businessLogic;
    private List<String> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClauseBreakdown {
        private String clause;
        private String explanation;
    }
}
