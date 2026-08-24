package com.example.querysence.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.querysence.model.dto.AnalyticsResponse;
import com.example.querysence.model.dto.QueryHistoryResponse;
import com.example.querysence.service.HistoryService;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/history")
    public ResponseEntity<Page<QueryHistoryResponse>> getHistory(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable) {
        return ResponseEntity.ok(historyService.getHistory(
                jwt.getSubject(), projectId, startDate, endDate, pageable));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<QueryHistoryResponse> getHistoryEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(historyService.getById(id, jwt.getSubject()));
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(historyService.getAnalytics(jwt.getSubject()));
    }

    @GetMapping("/analytics/slow-queries")
    public ResponseEntity<Page<QueryHistoryResponse>> getSlowQueries(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable) {
        return ResponseEntity.ok(historyService.getSlowQueries(jwt.getSubject(), pageable));
    }
}
