package com.example.querysence.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.example.querysence.model.dto.ExplainResponse;
import com.example.querysence.model.dto.NLToSQLRequest;
import com.example.querysence.model.dto.NLToSQLResponse;
import com.example.querysence.model.dto.OptimizationResponse;
import com.example.querysence.model.dto.SecurityScanRequest;
import com.example.querysence.model.dto.SecurityScanResponse;
import com.example.querysence.service.AIService;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/nl-to-sql")
    public ResponseEntity<NLToSQLResponse> naturalLanguageToSQL(
             @RequestBody NLToSQLRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.convertNaturalLanguageToSQL(request, authentication.getName()));
    }

    @PostMapping("/explain")
    public ResponseEntity<ExplainResponse> explainQuery(
            @RequestParam String sql,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.explainQuery(sql, authentication.getName()));
    }

    @PostMapping("/optimize")
    public ResponseEntity<OptimizationResponse> optimizeQuery(
            @RequestParam String sql,
            @RequestParam(required = false) Long schemaId,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.optimizeQuery(sql, schemaId, authentication.getName()));
    }

    @PostMapping("/security-scan")
    public ResponseEntity<SecurityScanResponse> securityScan(
           @RequestBody SecurityScanRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.scanForSecurity(request, authentication.getName()));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestParam String message,
            @RequestParam(required = false) Long schemaId,
            @RequestParam(required = false, defaultValue = "") String conversationHistory,
            Authentication authentication) {
        String response = aiService.chat(message, schemaId, conversationHistory, authentication.getName());
        return ResponseEntity.ok(Map.of("response", response));
    }
}
