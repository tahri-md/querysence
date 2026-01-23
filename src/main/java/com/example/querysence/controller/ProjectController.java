package com.example.querysence.controller;

import java.util.List;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.ProjectDto;
import com.example.querysence.model.dto.ProjectRequest;
import com.example.querysence.model.dto.SchemaDefinitionDto;
import com.example.querysence.model.dto.SchemaDefinitionRequest;
import com.example.querysence.service.SchemaManagementService;

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

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects() {
        return ResponseEntity.ok(smService.getAllProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable long id) {
        return ResponseEntity.ok(smService.getProject(id));
    }
    
    
    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody ProjectRequest  request,Authentication authentication) {
        return ResponseEntity.ok(smService.createProject(request,authentication.getName()));
    }
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/schemas")
    public ResponseEntity<SchemaDefinitionDto> createProjectSchema(@PathVariable long id,@RequestBody SchemaDefinitionRequest request) {
        
        return ResponseEntity.ok(smService.createProjectSchema(id,request));
    }
    

    
   
    
}
