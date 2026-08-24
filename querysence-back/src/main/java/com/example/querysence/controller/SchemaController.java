package com.example.querysence.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.SchemaCreateRequest;
import com.example.querysence.model.dto.SchemaResponse;
import com.example.querysence.model.dto.TableCreateRequest;
import com.example.querysence.service.SchemaManagementService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaManagementService schemaService;

    @PostMapping("/projects/{projectId}/schemas")
    @Operation(summary = "Create schema in project")
    public ResponseEntity<SchemaResponse> createSchema(
            @PathVariable Long projectId,
            @Valid @RequestBody SchemaCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(schemaService.createSchema(
                        projectId,
                        request,
                        keycloakUserId
                ));
    }

    @GetMapping("/projects/{projectId}/schemas")
    @Operation(summary = "List schemas in project")
    public ResponseEntity<List<SchemaResponse>> listSchemas(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();

        return ResponseEntity.ok(
                schemaService.getSchemasByProject(
                        projectId,
                        keycloakUserId
                )
        );
    }

    @GetMapping("/schemas/{id}")
    @Operation(summary = "Get schema with tables")
    public ResponseEntity<SchemaResponse> getSchema(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();

        return ResponseEntity.ok(
                schemaService.getSchema(
                        id,
                        keycloakUserId
                )
        );
    }

    @PostMapping("/schemas/{schemaId}/tables")
    @Operation(summary = "Add table to schema")
    public ResponseEntity<SchemaResponse> addTable(
            @PathVariable Long schemaId,
            @Valid @RequestBody TableCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(schemaService.addTable(
                        schemaId,
                        request,
                        keycloakUserId
                ));
    }

    @DeleteMapping("/schemas/{id}")
    @Operation(summary = "Delete schema")
    public ResponseEntity<Map<String, String>> deleteSchema(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();

        schemaService.deleteSchema(id, keycloakUserId);

        return ResponseEntity.ok(
                Map.of("message", "Schema deleted successfully")
        );
    }
}