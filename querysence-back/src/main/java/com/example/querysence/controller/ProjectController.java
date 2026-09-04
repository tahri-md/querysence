package com.example.querysence.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.ProjectCreateRequest;
import com.example.querysence.model.dto.ProjectResponse;
import com.example.querysence.service.SchemaManagementService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

        private final SchemaManagementService smService;

        @PostMapping
        @Operation(summary = "Create a new project")
        public ResponseEntity<ProjectResponse> createProject(
                        @Valid @RequestBody ProjectCreateRequest request,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(smService.create(request, keycloakUserId));
        }

        @GetMapping
        @Operation(summary = "List user's projects")
        public ResponseEntity<List<ProjectResponse>> listProjects(
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ResponseEntity.ok(
                                smService.listByUser(keycloakUserId));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get project details")
        public ResponseEntity<ProjectResponse> getProject(
                        @PathVariable Long id,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                return ResponseEntity.ok(
                                smService.getById(id, keycloakUserId));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a project")
        public ResponseEntity<Map<String, String>> deleteProject(
                        @PathVariable Long id,
                        @AuthenticationPrincipal Jwt jwt) {

                String keycloakUserId = jwt.getSubject();

                smService.delete(id, keycloakUserId);

                return ResponseEntity.ok(
                                Map.of("message", "Project deleted successfully"));
        }
}