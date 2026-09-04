package com.example.querysence.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.QueryExample;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.QueryExampleDTO;
import com.example.querysence.repository.QueryExampleRepository;
import com.example.querysence.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryRAGService {

    private final VectorStore vectorStore;
    private final QueryExampleRepository queryExampleRepository;
    private final UserRepository userRepository;

    @Value("${rag.retrieval.top-k:3}")
    private int topK;

    @Value("${rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.storage.min-score-to-store:0.8}")
    private double minScoreToStore;

    public List<QueryExampleDTO> findSimilarQueries(
            String nlQuery,
            SchemaDefinition schema) {

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(nlQuery)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression("schema_id == '" + schema.getId() + "'")
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            return results.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to find similar queries for: {}", nlQuery, e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public void storeSuccessfulQuery(
            String nlQuery,
            String sqlOutput,
            User user,
            SchemaDefinition schema,
            double confidenceScore) {

        try {
            if (confidenceScore < minScoreToStore) {
                log.debug(
                        "Skipping storage: confidence {} < threshold {}",
                        confidenceScore,
                        minScoreToStore);
                return;
            }

            String queryType = detectQueryType(sqlOutput);

            Document document = new Document(
                    nlQuery,
                    Map.of(
                            "sql_output", sqlOutput,
                            "schema_id", schema.getId().toString(),
                            "user_id", user.getId().toString(),
                            "query_type", queryType));

            vectorStore.add(List.of(document));

            QueryExample example = QueryExample.builder()
                    .nlQuery(nlQuery)
                    .sqlOutput(sqlOutput)
                    .user(user)
                    .schema(schema)
                    .confidenceScore(confidenceScore)
                    .verified(false)
                    .queryType(queryType)
                    .accessCount(0)
                    .vectorId(document.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            queryExampleRepository.save(example);

            log.info(
                    "Stored successful query: {} → {}",
                    nlQuery,
                    document.getId());

        } catch (Exception e) {
            log.error("Failed to store successful query", e);
        }
    }

    @Transactional
    public void verifyQuery(Long queryId, boolean verified) {
        QueryExample example = queryExampleRepository.findById(queryId)
                .orElseThrow();

        example.setVerified(verified);
        queryExampleRepository.save(example);

        log.info(
                "Query {} marked as verified: {}",
                queryId,
                verified);
    }

    @Transactional
    public void trackQueryUsage(Long queryId) {
        QueryExample example = queryExampleRepository.findById(queryId)
                .orElseThrow();

        example.setAccessCount(example.getAccessCount() + 1);
        example.setLastAccessedAt(LocalDateTime.now());

        queryExampleRepository.save(example);
    }

    public List<QueryExampleDTO> getUserSuggestions(String keycloakUserId, int limit) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return queryExampleRepository
                .findByUserIdAndVerifiedTrueOrderByAccessCountDesc(user.getId())
                .stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cleanupOldQueries(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);

        List<QueryExample> stale = queryExampleRepository.findRecentQueries(cutoff);

        List<String> staleIds = stale.stream()
                .map(QueryExample::getVectorId)
                .toList();

        if (!staleIds.isEmpty()) {
            vectorStore.delete(staleIds);
        }

        long deleted = queryExampleRepository.deleteByCreatedAtBefore(cutoff);

        log.info("Cleaned up {} old queries", deleted);
    }

    private String detectQueryType(String sql) {
        String upper = sql.toUpperCase().trim();

        if (upper.startsWith("SELECT")) {
            return "SELECT";
        }

        if (upper.contains("JOIN")) {
            return "JOIN";
        }

        if (upper.contains("GROUP BY")) {
            return "AGGREGATE";
        }

        if (upper.contains("UNION")) {
            return "UNION";
        }

        return "OTHER";
    }

    private QueryExampleDTO convertToDTO(Document doc) {
        return QueryExampleDTO.builder()
                .nlQuery(doc.getText())
                .sqlOutput((String) doc.getMetadata().get("sql_output"))
                .similarity(doc.getScore())
                .build();
    }

    private QueryExampleDTO convertToDTO(QueryExample example) {
        return QueryExampleDTO.builder()
                .id(example.getId())
                .nlQuery(example.getNlQuery())
                .sqlOutput(example.getSqlOutput())
                .verified(example.getVerified())
                .accessCount(example.getAccessCount())
                .build();
    }
}
