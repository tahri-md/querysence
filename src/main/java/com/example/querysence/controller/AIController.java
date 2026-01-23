package com.example.querysence.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.querysence.service.AIService;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/nl-to-sql")
    public ResponseEntity<com.example.querysence.model.dto.NLToSQLResponse> naturalLanguageToSQL(
             @RequestBody com.example.querysence.model.dto.NLToSQLRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.convertNaturalLanguageToSQL(request, authentication.getName()));
    }

    @PostMapping("/explain")
    public ResponseEntity<com.example.querysence.model.dto.ExplainResponse> explainQuery(
            @RequestParam String sql,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.explainQuery(sql, authentication.getName()));
    }

    @PostMapping("/optimize")
    public ResponseEntity<com.example.querysence.model.dto.OptimizationResponse> optimizeQuery(
            @RequestParam String sql,
            @RequestParam(required = false) Long schemaId,
            Authentication authentication) {
        return ResponseEntity.ok(aiService.optimizeQuery(sql, schemaId, authentication.getName()));
    }

    @PostMapping("/security-scan")
    public ResponseEntity<com.example.querysence.model.dto.SecurityScanResponse> securityScan(
           @RequestBody com.example.querysence.model.dto.SecurityScanRequest request,
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
