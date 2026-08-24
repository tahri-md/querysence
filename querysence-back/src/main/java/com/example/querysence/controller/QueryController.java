package com.example.querysence.controller;

import com.example.querysence.model.QueryParseResponse;
import com.example.querysence.model.dto.QueryAnalysisRequest;
import com.example.querysence.model.dto.QueryAnalysisResponse;
import com.example.querysence.service.QueryAnalysisService;
import com.example.querysence.service.QueryParserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queries")
@RequiredArgsConstructor
public class QueryController {

    private final QueryParserService queryParserService;
    private final QueryAnalysisService queryAnalysisService;

    @GetMapping("/parse")
    public ResponseEntity<QueryParseResponse> parseQuery(
            @RequestParam String sql,
            @RequestParam String dialect) {

        return ResponseEntity.ok(
                queryParserService.parseAndFormat(sql, dialect));
    }

    @PostMapping("/analyze")
    public ResponseEntity<QueryAnalysisResponse> analyzeQuery(
            @RequestBody QueryAnalysisRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();

        return ResponseEntity.ok(
                queryAnalysisService.analyze(
                        request,
                        keycloakUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryAnalysisResponse> getAnalysisById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                queryAnalysisService.getById(
                        id));
    }
}