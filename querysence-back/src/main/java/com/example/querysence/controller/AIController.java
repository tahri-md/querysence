package com.example.querysence.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.ExplainResponse;
import com.example.querysence.model.dto.NLToSQLRequest;
import com.example.querysence.model.dto.NLToSQLResponse;
import com.example.querysence.model.dto.OptimizationResponse;
import com.example.querysence.model.dto.QueryExampleDTO;
import com.example.querysence.model.dto.SecurityScanRequest;
import com.example.querysence.model.dto.SecurityScanResponse;
import com.example.querysence.service.AIService;
import com.example.querysence.service.QueryRAGService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

        private final AIService aiService;

        private final QueryRAGService ragService;

        @PostMapping("/nl-to-sql")
        public NLToSQLResponse naturalLanguageToSQLWithRAG(
                        @RequestBody NLToSQLRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                String keycloakUserId = jwt.getSubject();

                return aiService.convertNLToSQLWithRAG(
                                request,
                                keycloakUserId);
        }

        @GetMapping("/suggestions")
        public List<QueryExampleDTO> getSuggestions(
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ragService.getUserSuggestions(keycloakUserId, 10);
        }

        @PostMapping("/verify-query/{queryId}")
        public void verifyQuery(
                        @PathVariable Long queryId,
                        @RequestParam boolean verified,
                        @AuthenticationPrincipal Jwt jwt) {

                ragService.verifyQuery(queryId, verified);
        }

        // @PostMapping("/nl-to-sql")
        // public ResponseEntity<NLToSQLResponse> naturalLanguageToSQL(
        // @RequestBody NLToSQLRequest request,
        // @AuthenticationPrincipal Jwt jwt) {

        // String keycloakUserId = jwt.getSubject();

        // return ResponseEntity.ok(
        // aiService.convertNaturalLanguageToSQL(
        // request,
        // keycloakUserId
        // )
        // );
        // }

        @PostMapping("/explain")
        public ResponseEntity<ExplainResponse> explainQuery(
                        @RequestParam String sql,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ResponseEntity.ok(
                                aiService.explainQuery(
                                                sql,
                                                keycloakUserId));
        }

        @PostMapping("/optimize")
        public ResponseEntity<OptimizationResponse> optimizeQuery(
                        @RequestParam String sql,
                        @RequestParam(required = false) Long schemaId,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ResponseEntity.ok(
                                aiService.optimizeQuery(
                                                sql,
                                                schemaId,
                                                keycloakUserId));
        }

        @PostMapping("/security-scan")
        public ResponseEntity<SecurityScanResponse> securityScan(
                        @RequestBody SecurityScanRequest request,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ResponseEntity.ok(
                                aiService.scanForSecurity(
                                                request,
                                                keycloakUserId));
        }

        @PostMapping("/chat")
        public ResponseEntity<Map<String, String>> chat(
                        @RequestParam String message,
                        @RequestParam(required = false) Long schemaId,
                        @RequestParam(required = false, defaultValue = "") String conversationHistory,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                String response = aiService.chat(
                                message,
                                schemaId,
                                conversationHistory,
                                keycloakUserId);

                return ResponseEntity.ok(
                                Map.of("response", response));
        }
}