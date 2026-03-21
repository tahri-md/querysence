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

import com.example.querysence.model.ProjectRole;
import com.example.querysence.model.dto.AcceptInviteRequest;
import com.example.querysence.model.dto.CreateInviteRequest;
import com.example.querysence.model.dto.ProjectCreateRequest;
import com.example.querysence.model.dto.ProjectDto;
import com.example.querysence.model.dto.ProjectInviteDto;
import com.example.querysence.model.dto.ProjectMemberDto;
import com.example.querysence.model.dto.ProjectRequest;
import com.example.querysence.model.dto.ProjectResponse;
import com.example.querysence.model.dto.SchemaDefinitionDto;
import com.example.querysence.model.dto.SchemaDefinitionRequest;
import com.example.querysence.service.ProjectCollaborationService;
import com.example.querysence.service.SchemaManagementService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/projects")
public class ProjectController {
    @Autowired
    SchemaManagementService smService;

    @Autowired
    ProjectCollaborationService collaborationService;

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

    // Team Collaboration Endpoints

    @GetMapping("/{projectId}/members")
    @Operation(summary = "List project members")
    public ResponseEntity<List<ProjectMemberDto>> getProjectMembers(
            @PathVariable Long projectId,
            Authentication auth) {
        return ResponseEntity.ok(collaborationService.getProjectMembers(projectId));
    }

    @PostMapping("/{projectId}/members/invite")
    @Operation(summary = "Create an invite link for a project")
    public ResponseEntity<ProjectInviteDto> createInvite(
            @PathVariable Long projectId,
            @RequestBody CreateInviteRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collaborationService.createInvite(projectId, request, auth));
    }

    @PostMapping("/{projectId}/members/accept-invite")
    @Operation(summary = "Accept an invite link")
    public ResponseEntity<ProjectMemberDto> acceptInvite(
            @PathVariable Long projectId,
            @RequestBody AcceptInviteRequest request,
            Authentication auth) {
        return ResponseEntity.ok(collaborationService.acceptInvite(request.getInviteCode(), auth));
    }

    @PutMapping("/{projectId}/members/{memberId}/role")
    @Operation(summary = "Update member role")
    public ResponseEntity<ProjectMemberDto> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        ProjectRole newRole = ProjectRole.valueOf(request.get("role"));
        return ResponseEntity.ok(collaborationService.updateMemberRole(projectId, memberId, newRole, auth));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(summary = "Remove member from project")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            Authentication auth) {
        collaborationService.removeMember(projectId, memberId, auth);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @GetMapping("/{projectId}/invites")
    @Operation(summary = "List project invites")
    public ResponseEntity<List<ProjectInviteDto>> getProjectInvites(
            @PathVariable Long projectId,
            Authentication auth) {
        return ResponseEntity.ok(collaborationService.getProjectInvites(projectId, auth));
    }

    
   
    
}
