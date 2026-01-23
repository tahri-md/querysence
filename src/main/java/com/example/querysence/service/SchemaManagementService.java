package com.example.querysence.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.querysence.model.Project;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.TableDefinition;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.ProjectDto;
import com.example.querysence.model.dto.ProjectRequest;
import com.example.querysence.model.dto.SchemaDefinitionDto;
import com.example.querysence.model.dto.SchemaDefinitionRequest;
import com.example.querysence.model.dto.TableDefinitionDto;
import com.example.querysence.model.dto.TableDefinitionRequest;
import com.example.querysence.repository.ProjectRepository;
import com.example.querysence.repository.SchemaDefinitionRepository;
import com.example.querysence.repository.TableDefinitionRepository;
import com.example.querysence.repository.UserRepository;

@Service
public class SchemaManagementService {
    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    SchemaDefinitionRepository schemaDefinitionRepository;

    @Autowired
    TableDefinitionRepository tableDefinitionRepository;

    @Autowired
    UserRepository userRepository;

    public ProjectDto createProject(ProjectRequest request,String name) {
        User user = userRepository.findByFullName(name).orElseThrow();
        return mapProjectToDto(projectRepository.save(Project.builder()
                .name(request.getName())
                .owner(user)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream().map(SchemaManagementService::mapProjectToDto).toList();
    }

    public ProjectDto getProject(long id) {
        return mapProjectToDto(projectRepository.findById(id).orElseThrow());
    }

    public void deleteProject(long id) {
        projectRepository.deleteById(id);
    }

    public SchemaDefinitionDto createProjectSchema(long id,SchemaDefinitionRequest request) {
        Project project  = projectRepository.findById(id).orElseThrow();
        return mapSchemaToDto(schemaDefinitionRepository.save(SchemaDefinition.builder()
                                                                                .name(request.getName())
                                                                                .dialect(request.getDialect())
                                                                                .project(project)
                                                                                .createdAt(LocalDateTime.now())
                                                                                .updatedAt(LocalDateTime.now())
                                                                                .build()));

    }

    public SchemaDefinitionDto getSchema(long id) {
        return mapSchemaToDto(schemaDefinitionRepository.findById(id).orElseThrow());
    }
    public TableDefinitionDto createSchemaTable(TableDefinitionRequest request) {
        return mapTableToDto(tableDefinitionRepository.save(TableDefinition.builder()
                                                                            .tableName(request.getTableName())
                                                                            .description(request.getDescription())
                                                                            .estimatedRows(request.getEstimatedRows())
                                                                            .createdAt(LocalDateTime.now())
                                                                            .build()));
    }
    public void deleteSchema(long id) {
        schemaDefinitionRepository.deleteById(id);
    }
    public static TableDefinitionDto mapTableToDto(TableDefinition tableDefinition) {
        return TableDefinitionDto.builder()
                                    .tableName(tableDefinition.getTableName())
                                    .description(tableDefinition.getDescription())
                                    .estimatedRows(tableDefinition.getEstimatedRows())
                                    .columns(tableDefinition.getColumns())
                                    .indexes(tableDefinition.getIndexes())
                                    .build();                               
    }

    public static SchemaDefinitionDto mapSchemaToDto(SchemaDefinition schemaDefinition) {
        return SchemaDefinitionDto.builder()
                                    .name(schemaDefinition.getName())
                                    .dialect(schemaDefinition.getDialect())
                                    .createdAt(schemaDefinition.getCreatedAt())
                                    .updatedAt(schemaDefinition.getUpdatedAt())
                                    .build();
    }
    public static ProjectDto mapProjectToDto(Project project) {
        return ProjectDto.builder()
                .name(project.getName())
                .description(project.getDescription())
                .schemas(project.getSchemas())
                .owner(project.getOwner())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

}
