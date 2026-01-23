package com.example.querysence.service;
import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.IndexSuggestion;
import com.example.querysence.model.Project;
import com.example.querysence.model.QueryHistory;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.ComplexityReport;
import com.example.querysence.model.dto.IndexSuggestionResponse;
import com.example.querysence.model.dto.QueryAnalysisRequest;
import com.example.querysence.model.dto.QueryAnalysisResponse;
import com.example.querysence.model.dto.QueryParseResponse;

import com.example.querysence.parser.ParsedQuery;
import com.example.querysence.repository.ProjectRepository;
import com.example.querysence.repository.QueryHistoryRepository;
import com.example.querysence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryAnalysisService {

    private final QueryParserService parserService;
    private final ComplexityAnalyzerService complexityService;
    private final IndexAdvisorService indexAdvisorService;
    private final QueryHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public QueryAnalysisResponse analyze(QueryAnalysisRequest request, String userEmail) {
        User user = userRepository.findByFullName(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Parse the query
        ParsedQuery parsedQuery = parserService.parseQuery(request.getSql());
        
        // Analyze complexity
        ComplexityReport complexityReport = complexityService.analyze(parsedQuery);
        
        // Get index suggestions
        List<IndexSuggestionResponse> indexSuggestions = 
                indexAdvisorService.suggestIndexes(parsedQuery, request.getSchemaId());

        // Build warnings list
        List<String> warnings = new ArrayList<>(complexityReport.getWarnings());

        // Save to history
        QueryHistory history = saveToHistory(request, user, parsedQuery, complexityReport, indexSuggestions);

        // Build parse response
        com.example.querysence.model.QueryParseResponse parseResponse = parserService.parseAndFormat(request.getSql(), "POSTGRESQL");

        log.info("Analyzed query for user {}, complexity: {}", userEmail, complexityReport.getLevel());

        return QueryAnalysisResponse.builder()
                .queryId(history.getId())
                .queryType(parsedQuery.getQueryType())
                .complexity(complexityReport)
                .indexSuggestions(indexSuggestions)
                .warnings(warnings)
                .parseResult(parseResponse)
                .analyzedAt(history.getAnalyzedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public QueryAnalysisResponse getById(Long id, String fullName) {
        QueryHistory history = historyRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Query history", "id", id));

        // Verify ownership
        if (!history.getUser().getFullName().equals(fullName)) {
            throw new ResourceNotFoundException("Query history", "id", id);
        }

        // Re-analyze to get full details
        ParsedQuery parsedQuery = parserService.parseQuery(history.getQueryText());
        ComplexityReport complexityReport = complexityService.analyze(parsedQuery);
        
        List<IndexSuggestionResponse> suggestions = history.getIndexSuggestions().stream()
                .map(s -> IndexSuggestionResponse.builder()
                        .tableName(s.getTableName())
                        .columns(s.getColumns())
                        .suggestionType(s.getSuggestionType())
                        .impactScore(s.getImpactScore())
                        .reasoning(s.getReasoning())
                        .build())
                .toList();

        com.example.querysence.model.QueryParseResponse parseResponse = parserService.parseAndFormat(history.getQueryText(), "POSTGRESQL");

        return QueryAnalysisResponse.builder()
                .queryId(history.getId())
                .queryType(history.getQueryType())
                .complexity(complexityReport)
                .indexSuggestions(suggestions)
                .warnings(complexityReport.getWarnings())
                .parseResult(parseResponse)
                .analyzedAt(history.getAnalyzedAt())
                .build();
    }

    private QueryHistory saveToHistory(QueryAnalysisRequest request, User user, 
                                        ParsedQuery parsedQuery, ComplexityReport complexity,
                                        List<IndexSuggestionResponse> suggestions) {
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId()).orElse(null);
        }

        QueryHistory history = QueryHistory.builder()
                .user(user)
                .project(project)
                .queryText(request.getSql())
                .queryHash(parserService.computeQueryHash(request.getSql()))
                .queryType(parsedQuery.getQueryType())
                .complexityScore(complexity.getScore())
                .executionTimeMs(request.getExecutionTimeMs())
                .build();

        // Add index suggestions
        for (IndexSuggestionResponse suggestion : suggestions) {
            IndexSuggestion indexSuggestion = IndexSuggestion.builder()
                    .queryHistory(history)
                    .tableName(suggestion.getTableName())
                    .columns(suggestion.getColumns())
                    .suggestionType(suggestion.getSuggestionType())
                    .impactScore(suggestion.getImpactScore())
                    .reasoning(suggestion.getReasoning())
                    .build();
            history.getIndexSuggestions().add(indexSuggestion);
        }

        return historyRepository.save(history);
    }
}
