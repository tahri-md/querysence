package com.example.querysence.service;


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
import com.example.querysence.model.dto.SecurityScanRequest;
import com.example.querysence.model.dto.SecurityScanResponse;
import com.example.querysence.repository.AIUsageLogRepository;
import com.example.querysence.repository.SchemaDefinitionRepository;
import com.example.querysence.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Value("${ai.rate-limit.daily-requests:100}")
    private int dailyRequestLimit;

    @Transactional
    public NLToSQLResponse convertNaturalLanguageToSQL(NLToSQLRequest request, String username) {
        checkRateLimit(username);

        String schemaDescription = "";
        String dialect = "POSTGRESQL";
        
        if (request.getSchemaId() != null) {
            SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(request.getSchemaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", request.getSchemaId()));
            schemaDescription = buildSchemaDescription(schema);
            dialect = schema.getDialect();
        }

        String prompt = String.format(PromptTemplates.NL_TO_SQL_PROMPT, 
                schemaDescription, request.getQuery(), dialect);

        long startTime = System.currentTimeMillis();
        String generatedSql = callAI(prompt, username, "NL_TO_SQL");
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

        logUsage(username, "NL_TO_SQL", responseTime);

        return NLToSQLResponse.builder()
                .sql(generatedSql)
                .valid(isValid)
                .errorMessage(errorMessage)
                .dialect(dialect)
                .confidence(isValid ? 0.85 : 0.5)
                .build();
    }

    @Cacheable(value = "queryExplanations", key = "#sql.hashCode()")
    @Transactional
    public ExplainResponse explainQuery(String sql, String username) {
        checkRateLimit(username);

        String prompt = String.format(PromptTemplates.EXPLAIN_SQL_PROMPT, sql);

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, username, "EXPLAIN");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(username, "EXPLAIN", responseTime);

        try {
            return objectMapper.readValue(cleanJsonResponse(response), ExplainResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response for explain: {}", e.getMessage());
            return ExplainResponse.builder()
                    .summary("AI response parsing failed")
                    .businessLogic(response)
                    .build();
        }
    }

    @Transactional
    public OptimizationResponse optimizeQuery(String sql, Long schemaId, String username) {
        checkRateLimit(username);

        String schemaDescription = "";
        String tableStats = "No statistics available";
        
        if (schemaId != null) {
            SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(schemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));
            schemaDescription = buildSchemaDescription(schema);
            tableStats = buildTableStats(schema);
        }

        String prompt = String.format(PromptTemplates.OPTIMIZE_SQL_PROMPT, sql, schemaDescription, tableStats);

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, username, "OPTIMIZE");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(username, "OPTIMIZE", responseTime);

        try {
            return objectMapper.readValue(cleanJsonResponse(response), OptimizationResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response for optimize: {}", e.getMessage());
            return OptimizationResponse.builder()
                    .overallAssessment(response)
                    .build();
        }
    }

    @Transactional
    public SecurityScanResponse scanForSecurity(SecurityScanRequest request, String username) {
        checkRateLimit(username);

        String prompt = String.format(PromptTemplates.SECURITY_SCAN_PROMPT, 
                request.getCode(), request.getContext());

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, username, "SECURITY_SCAN");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(username, "SECURITY_SCAN", responseTime);

        try {
            return objectMapper.readValue(cleanJsonResponse(response), SecurityScanResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response for security scan: {}", e.getMessage());
            return SecurityScanResponse.builder()
                    .summary(response)
                    .riskScore(50)
                    .build();
        }
    }

    @Transactional
    public String chat(String message, Long schemaId, String conversationHistory, String username) {
        checkRateLimit(username);

        String schemaDescription = "No schema loaded";
        if (schemaId != null) {
            SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(schemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));
            schemaDescription = buildSchemaDescription(schema);
        }

        String prompt = String.format(PromptTemplates.CHAT_CONTEXT_PROMPT, 
                schemaDescription, conversationHistory, message);

        long startTime = System.currentTimeMillis();
        String response = callAI(prompt, username, "CHAT");
        long responseTime = System.currentTimeMillis() - startTime;

        logUsage(username, "CHAT", responseTime);

        return response;
    }

   private String callAI(String prompt, String username, String feature) {
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
            if (retries == 0) throw new AIServiceException(
                "AI service temporarily unavailable. Please try again later.");
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }
    }
    throw new AIServiceException("Unexpected AI service failure.");
}


    private void checkRateLimit(String username) {
        User user = userRepository.findByFullName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime dayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long requestCount = aiUsageLogRepository.countByUserSince(user, dayStart);

        if (requestCount >= dailyRequestLimit) {
            throw new BadRequestException("Daily AI request limit exceeded. Limit: " + dailyRequestLimit);
        }
    }

    private void logUsage(String username, String feature, long responseTimeMs) {
        try {
            User user = userRepository.findByFullName(username).orElse(null);
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
                if (col.getIsPrimaryKey()) sb.append(" PRIMARY KEY");
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
        if (response == null) return "";
        return response
                .replaceAll("```sql\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
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
