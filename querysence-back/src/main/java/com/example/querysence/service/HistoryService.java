package com.example.querysence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.Project;
import com.example.querysence.model.QueryHistory;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.AnalyticsResponse;
import com.example.querysence.model.dto.QueryHistoryResponse;
import com.example.querysence.repository.ProjectRepository;
import com.example.querysence.repository.QueryHistoryRepository;
import com.example.querysence.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final QueryHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public Page<QueryHistoryResponse> getHistory(String username, Long projectId,
                                                  LocalDateTime startDate, LocalDateTime endDate,
                                                  Pageable pageable) {
        User user = getUser(username);
        Page<QueryHistory> historyPage;

        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
            historyPage = historyRepository.findByUserAndProject(user, project, pageable);
        } else if (startDate != null && endDate != null) {
            historyPage = historyRepository.findByUserAndAnalyzedAtBetween(user, startDate, endDate, pageable);
        } else {
            historyPage = historyRepository.findByUser(user, pageable);
        }

        return historyPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public QueryHistoryResponse getById(Long id, String username) {
        QueryHistory history = historyRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Query history", "id", id));

        if (!history.getUser().getFullName().equals(username)) {
            throw new ResourceNotFoundException("Query history", "id", id);
        }

        return mapToResponseWithDetails(history);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String username) {
        User user = getUser(username);

        Long totalQueries = historyRepository.countByUser(user);
        Double avgComplexity = historyRepository.getAverageComplexityByUser(user);

        // Get query type distribution
        List<Object[]> typeDistribution = historyRepository.countByUserGroupByQueryType(user);
        Map<String, Long> queryTypeStats = typeDistribution.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Get query trend (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> trendData = historyRepository.getQueryTrendByUser(user, thirtyDaysAgo);
        List<AnalyticsResponse.TrendPoint> trend = trendData.stream()
                .map(arr -> AnalyticsResponse.TrendPoint.builder()
                        .date(arr[0] instanceof LocalDate ? (LocalDate) arr[0] : LocalDate.parse(arr[0].toString()))
                        .count(((Number) arr[1]).intValue())
                        .build())
                .toList();

        return AnalyticsResponse.builder()
                .totalQueries(totalQueries != null ? totalQueries : 0)
                .avgComplexity(avgComplexity != null ? avgComplexity.intValue() : 0)
                .queryTypeDistribution(queryTypeStats)
                .queryTrend(trend)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<QueryHistoryResponse> getSlowQueries(String username, Pageable pageable) {
        User user = getUser(username);
        List<QueryHistory> slowQueries = historyRepository.findSlowQueriesByUser(user);
        
        List<QueryHistoryResponse> responses = slowQueries.stream()
                .map(this::mapToResponse)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        
        if (start > responses.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, responses.size());
        }
        
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    private User getUser(String username) {
        return userRepository.findByFullName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private QueryHistoryResponse mapToResponse(QueryHistory history) {
        return QueryHistoryResponse.builder()
                .id(history.getId())
                .queryText(history.getQueryText())
                .queryType(history.getQueryType())
                .complexityScore(history.getComplexityScore())
                .executionTimeMs(history.getExecutionTimeMs())
                .projectId(history.getProject() != null ? history.getProject().getId() : null)
                .projectName(history.getProject() != null ? history.getProject().getName() : null)
                .analyzedAt(history.getAnalyzedAt())
                .build();
    }

    private QueryHistoryResponse mapToResponseWithDetails(QueryHistory history) {
        QueryHistoryResponse response = mapToResponse(history);
        
   response.setIndexSuggestions(
    history.getIndexSuggestions().stream()
        .map(s -> QueryHistoryResponse.IndexSuggestionSummary.builder()
            .tableName(s.getTableName())
            .columns(s.getColumns())
            .impactScore(
                s.getImpactScore() != null 
                    ? s.getImpactScore().toString() 
                    : null
            )
            .build()
        )
        .toList()
);

response.setSecurityFindings(
    history.getSecurityFindings().stream()
        .map(f -> QueryHistoryResponse.SecurityFindingSummary.builder()
            .type(f.getFindingType())
            .severity(
                f.getSeverity() != null 
                    ? f.getSeverity().name() 
                    : null
            )
            .description(f.getDescription())
            .build()
        )
        .toList()
);

        
        return response;
    }
}
