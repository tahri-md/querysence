package com.example.querysence.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.QueryParseResponse;
import com.example.querysence.model.dto.QueryAnalysisRequest;
import com.example.querysence.model.dto.QueryAnalysisResponse;
import com.example.querysence.service.QueryAnalysisService;
import com.example.querysence.service.QueryParserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/queries")
public class QueryController {
    @Autowired
    QueryParserService queryParserService;
    @Autowired
    QueryAnalysisService queryAnalysisService;
    @GetMapping("/parse")
    public ResponseEntity<QueryParseResponse> parseQuery(@RequestParam String sql,@RequestParam String dialect) {
        return ResponseEntity.ok(queryParserService.parseAndFormat(sql, dialect));
    }
     @PostMapping("/analyze")
    public ResponseEntity<QueryAnalysisResponse> analyzeQuery(
            @RequestBody QueryAnalysisRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(queryAnalysisService.analyze(request, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryAnalysisResponse> getAnalysisById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(queryAnalysisService.getById(id, authentication.getName()));
    }
    
}

