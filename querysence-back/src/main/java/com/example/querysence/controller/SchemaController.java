package com.example.querysence.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.SchemaCreateRequest;
import com.example.querysence.model.dto.SchemaDefinitionDto;
import com.example.querysence.model.dto.SchemaResponse;
import com.example.querysence.model.dto.TableCreateRequest;
import com.example.querysence.model.dto.TableDefinitionDto;
import com.example.querysence.model.dto.TableDefinitionRequest;
import com.example.querysence.service.SchemaManagementService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
public class SchemaController {
    @Autowired
    SchemaManagementService schemaService;

   @PostMapping("/projects/{projectId}/schemas")
    @Operation(summary = "Create schema in project")
    public ResponseEntity<SchemaResponse> createSchema(
            @PathVariable Long projectId,
            @Valid @RequestBody SchemaCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.createSchema(projectId, request, userDetails.getUsername()));
    }

    @GetMapping("/projects/{projectId}/schemas")
    @Operation(summary = "List schemas in project")
    public ResponseEntity<List<SchemaResponse>> listSchemas(@PathVariable Long projectId) {
        return ResponseEntity.ok(schemaService.getSchemasByProject(projectId));
    }

    @GetMapping("/schemas/{id}")
    @Operation(summary = "Get schema with tables")
    public ResponseEntity<SchemaResponse> getSchema(@PathVariable Long id) {
        return ResponseEntity.ok(schemaService.getSchema(id));
    }

    @PostMapping("/schemas/{schemaId}/tables")
    @Operation(summary = "Add table to schema")
    public ResponseEntity<SchemaResponse> addTable(
            @PathVariable Long schemaId,
            @Valid @RequestBody TableCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.addTable(schemaId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/schemas/{id}")
    @Operation(summary = "Delete schema")
    public ResponseEntity<Map<String, String>> deleteSchema(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        schemaService.deleteSchema(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Schema deleted successfully"));
    }
    
}
