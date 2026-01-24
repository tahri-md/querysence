package com.example.querysence.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.ProjectCreateRequest;
import com.example.querysence.model.dto.ProjectDto;
import com.example.querysence.model.dto.ProjectRequest;
import com.example.querysence.model.dto.ProjectResponse;
import com.example.querysence.model.dto.SchemaDefinitionDto;
import com.example.querysence.model.dto.SchemaDefinitionRequest;
import com.example.querysence.service.SchemaManagementService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/projects")
public class ProjectController {
    @Autowired
    SchemaManagementService smService;

        @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(smService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List user's projects")
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(smService.listByUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project details")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(smService.getById(id, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<Map<String, String>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        smService.delete(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }

    
   
    
}
