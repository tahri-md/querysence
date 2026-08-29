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
public class ExplainResponse {
    
    private String summary;
    private List<ClauseBreakdown> breakdown;
    private String businessLogic;
    private List<String> suggestions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClauseBreakdown {
        private String clause;
        private String explanation;
    }
}
