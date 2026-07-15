package com.example.querysence.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.DbConnectionDto;
import com.example.querysence.model.dto.DbConnectionRequest;
import com.example.querysence.model.dto.SchemaSyncResponse;
import com.example.querysence.model.dto.TestConnectionResponse;
import com.example.querysence.service.DbConnectionService;
import com.example.querysence.service.SchemaIntrospectionService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/db-connections")
@RequiredArgsConstructor
public class DbConnectionController {

    private final DbConnectionService dbConnectionService;
    private final SchemaIntrospectionService schemaIntrospectionService;

    @PostMapping
    @Operation(summary = "Add a live database connection to a project")
    public ResponseEntity<DbConnectionDto> createConnection(
            @PathVariable Long projectId,
            @Valid @RequestBody DbConnectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dbConnectionService.create(projectId, request, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List database connections for a project")
    public ResponseEntity<List<DbConnectionDto>> listConnections(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dbConnectionService.listByProject(projectId, userDetails.getUsername()));
    }

    @DeleteMapping("/{connectionId}")
    @Operation(summary = "Delete a database connection")
    public ResponseEntity<Map<String, String>> deleteConnection(
            @PathVariable Long projectId,
            @PathVariable Long connectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        dbConnectionService.delete(projectId, connectionId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Connection deleted successfully"));
    }

    @PostMapping("/{connectionId}/test")
    @Operation(summary = "Test a database connection")
    public ResponseEntity<TestConnectionResponse> testConnection(
            @PathVariable Long projectId,
            @PathVariable Long connectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dbConnectionService.testConnection(projectId, connectionId, userDetails.getUsername()));
    }

    @PostMapping("/{connectionId}/sync-schema")
    @Operation(summary = "Introspect the live database and sync it into a SchemaDefinition")
    public ResponseEntity<SchemaSyncResponse> syncSchema(
            @PathVariable Long projectId,
            @PathVariable Long connectionId,
            @RequestParam(required = false) Long schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                schemaIntrospectionService.syncSchema(projectId, connectionId, schemaId, userDetails.getUsername()));
    }
}
