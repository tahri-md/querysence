package com.example.querysence.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.querysence.ai.PromptTemplates;
import com.example.querysence.exception.AIServiceException;
import com.example.querysence.exception.BadRequestException;
import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.AIUsageLog;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.ExplainResponse;
import com.example.querysence.model.dto.NLToSQLRequest;
import com.example.querysence.model.dto.NLToSQLResponse;
import com.example.querysence.model.dto.OptimizationResponse;
import com.example.querysence.model.dto.QueryExampleDTO;
import com.example.querysence.model.dto.SecurityScanRequest;
import com.example.querysence.model.dto.SecurityScanResponse;
import com.example.querysence.repository.AIUsageLogRepository;
import com.example.querysence.repository.SchemaDefinitionRepository;
import com.example.querysence.repository.UserRepository;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final ChatClient.Builder chatClientBuilder;
    private final SchemaDefinitionRepository schemaRepository;
    private final UserRepository userRepository;
    private final AIUsageLogRepository aiUsageLogRepository;
    private final ObjectMapper objectMapper;
    private final QueryParserService queryParserService;
    private final QueryRAGService ragService;

    @Value("${ai.rate-limit.daily-requests:100}")
    private int dailyRequestLimit;

    public NLToSQLResponse convertNLToSQLWithRAG(
            NLToSQLRequest request,
            String keycloakUserId) {

        try {
            User user = userRepository.findByKeycloakUserId(keycloakUserId)
                    .orElseThrow();

            SchemaDefinition schema = schemaRepository
                    .findByIdWithFullDetails(request.getSchemaId())
                    .orElseThrow();

            List<QueryExampleDTO> examples = ragService.findSimilarQueries(
                    request.getQuery(),
                    schema);

            String enhancedPrompt = buildPromptWithExamples(
                    request.getQuery(),
                    schema,
                    examples);

            long startTime = System.currentTimeMillis();

            String response = callAI(
                    enhancedPrompt,
                    "NL_TO_SQL_RAG");

            long responseTime = System.currentTimeMillis() - startTime;

            String generatedSql = cleanSqlResponse(response);

            boolean isValid = true;
            String errorMessage = null;
            double confidence = 0.85;

            try {
                queryParserService.parseQuery(generatedSql);
                confidence = examples.isEmpty() ? 0.75 : 0.90;
            } catch (Exception e) {
                isValid = false;
                errorMessage = e.getMessage();
                confidence = 0.4;
            }

            if (isValid) {
                ragService.storeSuccessfulQuery(
                        request.getQuery(),
                        generatedSql,
                        user,
                        schema,
                        confidence);
            }

            logUsage(
                    keycloakUserId,
                    "NL_TO_SQL_RAG",
                    responseTime);

            return NLToSQLResponse.builder()
                    .sql(generatedSql)
                    .valid(isValid)
                    .errorMessage(errorMessage)
                    .dialect(schema.getDialect())
                    .confidence(confidence)
                    .examplesUsed(examples.size())
                    .build();

        } catch (Exception e) {
            log.error(
                    "Failed to convert NL to SQL with RAG",
                    e);

            throw new AIServiceException(e.getMessage());
        }
    }

    private String buildPromptWithExamples(
            String nlQuery,
            SchemaDefinition schema,
            List<QueryExampleDTO> examples) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are a SQL expert. Convert natural language to SQL.

                Database: %s
                Dialect: %s
                """.formatted(
                schema.getName(),
                schema.getDialect()));

        prompt.append("\nSchema:\n");
        prompt.append(buildSchemaDescription(schema));

        if (!examples.isEmpty()) {
            prompt.append("\nExamples of similar queries:\n");

            for (int i = 0; i < examples.size(); i++) {
                QueryExampleDTO example = examples.get(i);

                prompt.append(String.format(
                        "Example %d:\nInput: %s\nOutput: %s\n\n",
                        i + 1,
                        example.getNlQuery(),
                        example.getSqlOutput()));
            }
        }

        prompt.append(String.format(
                "\nNow convert this to SQL:\nInput: %s\nOutput: ",
                nlQuery));

        return prompt.toString();
    }

    @Transactional
    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "\\n in prompt text blocks is intentional for AI prompt formatting")
    public NLToSQLResponse convertNaturalLanguageToSQL(NLToSQLRequest request, String keycloakUserId) {
        checkRateLimit(keycloakUserId);

        String schemaDescription = "";
        String dialect = "POSTGRESQL";

        if (request.getSchemaId() != null) {
            SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(request.getSchemaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", request.getSchemaId()));
            schemaDescription = buildSchemaDescription(schema);
            dialect = schema.getDialect();
        }

        String prompt = PromptTemplates.NL_TO_SQL_PROMPT.formatted(
                schemaDescription, request.getQuery(), dialect);

        long startTime = System.currentTimeMillis();
        String generatedSql = callAI(prompt, "NL_TO_SQL");
        long responseTime = System.currentTimeMillis() - startTime;

        generatedSql = cleanSqlResponse(generatedSql);

        boolean isValid = true;
        String errorMessage = null;
        try {
            queryParserService.parseQuery(generatedSql);
        } catch (Exception e) {
            isValid = false;
            errorMessage = e.getMessage();
        }

        logUsage(keycloakUserId, "NL_TO_SQL", responseTime);

        return NLToSQLResponse.builder()
                .sql(generatedSql)
                .valid(isValid)
                .errorMessage(errorMessage)
                .dialect(dialect)
                .confidence(isValid ? 0.85 : 0.5)
                .build();
    }

    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "\\n in prompt text blocks is intentional for AI prompt formatting")
    @Cacheable(value = "queryExplanations", key = "#sql.hashCode()")
    @Transactional
    public ExplainResponse explainQuery(String sql, String keycloakUserId) {
        checkRateLimit(keycloakUserId);

        String prompt = PromptTemplates.EXPLAIN_SQL_PROMPT.formatted(sql);

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, "EXPLAIN");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(keycloakUserId, "EXPLAIN", responseTime);

        try {
            return objectMapper.readValue(cleanJsonResponse(response), ExplainResponse.class);
        } catch (JacksonException e) {
            log.error("Failed to parse AI response for explain: {}", e.getMessage());
            return ExplainResponse.builder()
                    .summary("AI response parsing failed")
                    .businessLogic(response)
                    .build();
        }
    }

    @Transactional
    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "\\n in prompt text blocks is intentional for AI prompt formatting")
    public OptimizationResponse optimizeQuery(String sql, Long schemaId, String keycloakUserId) {
        checkRateLimit(keycloakUserId);

        String schemaDescription = "";
        String tableStats = "No statistics available";

        if (schemaId != null) {
            SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(schemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));
            schemaDescription = buildSchemaDescription(schema);
            tableStats = buildTableStats(schema);
        }

        String prompt = PromptTemplates.OPTIMIZE_SQL_PROMPT.formatted(sql, schemaDescription, tableStats);

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, "OPTIMIZE");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(keycloakUserId, "OPTIMIZE", responseTime);

        try {
            return objectMapper.readValue(cleanJsonResponse(response), OptimizationResponse.class);
        } catch (JacksonException e) {
            log.error("Failed to parse AI response for optimize: {}", e.getMessage());
            return OptimizationResponse.builder()
                    .overallAssessment(response)
                    .build();
        }
    }

    @Transactional
    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "\\n in prompt text blocks is intentional for AI prompt formatting")
    public SecurityScanResponse scanForSecurity(SecurityScanRequest request, String keycloakUserId) {
        checkRateLimit(keycloakUserId);

        String prompt = PromptTemplates.SECURITY_SCAN_PROMPT.formatted(
                request.getCode(), request.getContext());

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, "SECURITY_SCAN");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(keycloakUserId, "SECURITY_SCAN", responseTime);

        try {
            return objectMapper.readValue(cleanJsonResponse(response), SecurityScanResponse.class);
        } catch (JacksonException e) {
            log.error("Failed to parse AI response for security scan: {}", e.getMessage());
            return SecurityScanResponse.builder()
                    .summary(response)
                    .riskScore(50)
                    .build();
        }
    }

    @Transactional
    public String chat(String message, Long schemaId, String conversationHistory, String keycloakUserId) {
        checkRateLimit(keycloakUserId);

        String schemaDescription = "No schema loaded";
        if (schemaId != null) {
            SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(schemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));
            schemaDescription = buildSchemaDescription(schema);
        }

        String prompt = PromptTemplates.CHAT_CONTEXT_PROMPT.formatted(
                schemaDescription, conversationHistory, message);

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, "CHAT");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(keycloakUserId, "CHAT", responseTime);

        return response;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private String callAI(String prompt, String feature) {
        int retries = 3;
        while (retries > 0) {
            try {
                ChatClient chatClient = chatClientBuilder.build();
                return chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
            } catch (Exception e) {
                retries--;
                log.warn("AI call failed ({}), retries left {}: {}", feature, retries, e.getMessage());
                if (retries == 0) {
                    throw new AIServiceException(
                            "AI service temporarily unavailable. Please try again later.");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new AIServiceException("Unexpected AI service failure.");
    }

    private void checkRateLimit(String keycloakUserId) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime dayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long requestCount = aiUsageLogRepository.countByUserSince(user, dayStart);

        if (requestCount >= dailyRequestLimit) {
            throw new BadRequestException("Daily AI request limit exceeded. Limit: " + dailyRequestLimit);
        }
    }

    private void logUsage(String keycloakUserId, String feature, long responseTimeMs) {
        try {
            User user = userRepository.findByKeycloakUserId(keycloakUserId).orElse(null);
            AIUsageLog log = AIUsageLog.builder()
                    .user(user)
                    .feature(feature)
                    .responseTimeMs(responseTimeMs)
                    .modelUsed("gpt-4")
                    .build();
            aiUsageLogRepository.save(log);
        } catch (Exception e) {
            log.warn("Failed to log AI usage: {}", e.getMessage());
        }
    }

    private String buildSchemaDescription(SchemaDefinition schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Database: ").append(schema.getName()).append("\n");
        sb.append("Dialect: ").append(schema.getDialect()).append("\n\n");

        for (var table : schema.getTables()) {
            sb.append("Table: ").append(table.getTableName()).append("\n");
            sb.append("Columns:\n");
            for (var col : table.getColumns()) {
                sb.append("  - ").append(col.getColumnName())
                        .append(" (").append(col.getDataType()).append(")");
                if (col.getIsPrimaryKey()) {
                    sb.append(" PRIMARY KEY");
                }
                if (col.getIsForeignKey()) {
                    sb.append(" REFERENCES ").append(col.getReferencesTable())
                            .append("(").append(col.getReferencesColumn()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildTableStats(SchemaDefinition schema) {
        StringBuilder sb = new StringBuilder();
        for (var table : schema.getTables()) {
            sb.append(table.getTableName()).append(": ")
                    .append(table.getEstimatedRows()).append(" rows\n");
        }
        return sb.toString();
    }

    private String cleanSqlResponse(String response) {
        if (response == null) {
            return "";
        }
        return response
                .replaceAll("```sql\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }
        String cleaned = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
