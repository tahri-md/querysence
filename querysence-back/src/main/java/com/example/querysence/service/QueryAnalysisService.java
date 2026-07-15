package com.example.querysence.service;

import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.DbConnection;
import com.example.querysence.model.ExecutionPlan;
import com.example.querysence.model.IndexSuggestion;
import com.example.querysence.model.Project;
import com.example.querysence.model.QueryHistory;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.ComplexityReport;
import com.example.querysence.model.dto.ExecutionPlanDto;
import com.example.querysence.model.dto.IndexSuggestionResponse;
import com.example.querysence.model.dto.QueryAnalysisRequest;
import com.example.querysence.model.dto.QueryAnalysisResponse;

import com.example.querysence.parser.ParsedQuery;
import com.example.querysence.repository.DbConnectionRepository;
import com.example.querysence.repository.ExecutionPlanRepository;
import com.example.querysence.repository.ProjectRepository;
import com.example.querysence.repository.QueryHistoryRepository;
import com.example.querysence.repository.SchemaDefinitionRepository;
import com.example.querysence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private final DbConnectionRepository dbConnectionRepository;
    private final ExecutionPlanRepository executionPlanRepository;
    private final QueryExecutionPlanService queryExecutionPlanService;
    private final SchemaDefinitionRepository schemaDefinitionRepository;

    @Transactional
    public QueryAnalysisResponse analyze(QueryAnalysisRequest request, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ParsedQuery parsedQuery = parserService.parseQuery(request.getSql());

        ComplexityReport complexityReport = complexityService.analyze(parsedQuery, request.getSchemaId());

        List<IndexSuggestionResponse> indexSuggestions =
                indexAdvisorService.suggestIndexes(parsedQuery, request.getSchemaId());

        List<String> warnings = new ArrayList<>(complexityReport.getWarnings());

        QueryHistory history = saveToHistory(request, user, parsedQuery, complexityReport, indexSuggestions);

        ExecutionPlanDto executionPlanDto = null;
        if (request.getDbConnectionId() != null) {
            executionPlanDto = runLivePlanAndAttach(history, request.getDbConnectionId(), parsedQuery.getQueryType(), warnings);
        }

        com.example.querysence.model.QueryParseResponse parseResponse = parserService.parseAndFormat(request.getSql(), "POSTGRESQL");

        log.info("Analyzed query for user {}, complexity: {}", username, complexityReport.getLevel());

        return QueryAnalysisResponse.builder()
                .queryId(history.getId())
                .queryType(parsedQuery.getQueryType())
                .complexity(complexityReport)
                .indexSuggestions(indexSuggestions)
                .warnings(warnings)
                .parseResult(parseResponse)
                .dbConnectionId(request.getDbConnectionId())
                .executionPlan(executionPlanDto)
                .analyzedAt(history.getAnalyzedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public QueryAnalysisResponse getById(Long id, String fullName) {
        QueryHistory history = historyRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Query history", "id", id));

        if (!history.getUser().getFullName().equals(fullName)) {
            throw new ResourceNotFoundException("Query history", "id", id);
        }

        ParsedQuery parsedQuery = parserService.parseQuery(history.getQueryText());
        Long schemaId = history.getSchema() != null ? history.getSchema().getId() : null;
        ComplexityReport complexityReport = complexityService.analyze(parsedQuery, schemaId);

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

        ExecutionPlanDto executionPlanDto = executionPlanRepository.findByQueryHistoryId(id)
                .map(this::mapPlanToDto)
                .orElse(null);

        return QueryAnalysisResponse.builder()
                .queryId(history.getId())
                .queryType(history.getQueryType())
                .complexity(complexityReport)
                .indexSuggestions(suggestions)
                .warnings(complexityReport.getWarnings())
                .parseResult(parseResponse)
                .dbConnectionId(history.getDbConnection() != null ? history.getDbConnection().getId() : null)
                .executionPlan(executionPlanDto)
                .analyzedAt(history.getAnalyzedAt())
                .build();
    }

    private ExecutionPlanDto runLivePlanAndAttach(QueryHistory history, Long dbConnectionId, String queryType,
                                                   List<String> warnings) {
        try {
            DbConnection connection = dbConnectionRepository.findById(dbConnectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("DbConnection", "id", dbConnectionId));

            ExecutionPlan plan = queryExecutionPlanService.runLivePlan(history, connection, history.getQueryText(), queryType);
            plan = executionPlanRepository.save(plan);

            history.setDbConnection(connection);
            history.setExecutionPlan(plan);
            historyRepository.save(history);

            return mapPlanToDto(plan);
        } catch (Exception e) {
            log.warn("Live EXPLAIN failed for query history {}, falling back to static analysis only: {}",
                    history.getId(), e.getMessage());
            warnings.add("Live EXPLAIN could not be run against the connected database: " + e.getMessage()
                    + " - showing static analysis only");
            return null;
        }
    }

    private ExecutionPlanDto mapPlanToDto(ExecutionPlan plan) {
        return ExecutionPlanDto.builder()
                .id(plan.getId())
                .source(plan.getSource().name())
                .planText(plan.getPlanText())
                .estimatedCost(plan.getEstimatedCost())
                .actualRows(plan.getActualRows())
                .actualTimeMs(plan.getActualTimeMs())
                .usedIndexes(plan.getUsedIndexes())
                .fullTableScans(plan.getFullTableScans())
                .build();
    }

    private QueryHistory saveToHistory(QueryAnalysisRequest request, User user,
                                        ParsedQuery parsedQuery, ComplexityReport complexity,
                                        List<IndexSuggestionResponse> suggestions) {
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId()).orElseThrow(()->new RuntimeException("project not found"));
        }

        SchemaDefinition schema = null;
        if (request.getSchemaId() != null) {
            schema = schemaDefinitionRepository.findById(request.getSchemaId()).orElse(null);
        }

        QueryHistory history = QueryHistory.builder()
                .user(user)
                .project(project)
                .schema(schema)
                .queryText(request.getSql())
                .queryHash(parserService.computeQueryHash(request.getSql()))
                .queryType(parsedQuery.getQueryType())
                .complexityScore(complexity.getScore())
                .executionTimeMs(request.getExecutionTimeMs())
                .analyzedAt(LocalDateTime.now())
                .build();

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