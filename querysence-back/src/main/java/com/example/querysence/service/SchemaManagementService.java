package com.example.querysence.service;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.querysence.exception.BadRequestException;
import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.ColumnDefinition;
import com.example.querysence.model.IndexDefinition;
import com.example.querysence.model.Project;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.TableDefinition;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.ProjectCreateRequest;

import com.example.querysence.model.dto.ProjectResponse;
import com.example.querysence.model.dto.SchemaCreateRequest;
import com.example.querysence.model.dto.SchemaResponse;
import com.example.querysence.model.dto.TableCreateRequest;
import com.example.querysence.repository.ProjectRepository;
import com.example.querysence.repository.SchemaDefinitionRepository;
import com.example.querysence.repository.TableDefinitionRepository;
import com.example.querysence.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchemaManagementService {
    @Autowired
    private  ProjectRepository projectRepository;
        @Autowired
    private  UserRepository userRepository;
        @Autowired
    private  SchemaDefinitionRepository schemaRepository;
        @Autowired
    private  TableDefinitionRepository tableRepository;

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request, String username) {
        User user = userRepository.findByFullName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (projectRepository.existsByNameAndOwner(request.getName(), user)) {
            throw new BadRequestException("Project with this name already exists");
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(user)
                .build();

        project = projectRepository.save(project);

        return mapToResponse(project);
    }

    @Transactional
    public List<ProjectResponse> listByUser(String username) {
        User user = userRepository.findByFullName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return projectRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponseWithSchemas)
                .toList();
    }

    @Transactional
    public ProjectResponse getById(Long id, String username) {
        Project project = projectRepository.findByIdWithSchemas(id);
        if (project == null) {
            throw new ResourceNotFoundException("Project", "id", id);
        }

        if (!project.getOwner().getFullName().equals(username)) {
            throw new ResourceNotFoundException("Project", "id", id);
        }

        return mapToResponseWithSchemas(project);
    }

    @Transactional
    public void delete(Long id, String username) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        if (!project.getOwner().getFullName().equals(username)) {
            throw new BadRequestException("You don't have permission to delete this project");
        }

        projectRepository.delete(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .schemaCount(project.getSchemas().size())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectResponse mapToResponseWithSchemas(Project project) {
        List<SchemaResponse> schemas = project.getSchemas().stream()
                .map(s -> SchemaResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .dialect(s.getDialect())
                        .projectId(project.getId())
                        .createdAt(s.getCreatedAt())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .toList();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .schemaCount(schemas.size())
                .schemas(schemas)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

   

    @Transactional
    public SchemaResponse createSchema(Long projectId, SchemaCreateRequest request, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getOwner().getFullName().equals(username)) {
            throw new BadRequestException("You don't have permission to modify this project");
        }

        if (schemaRepository.existsByNameAndProject(request.getName(), project)) {
            throw new BadRequestException("Schema with this name already exists in the project");
        }

        SchemaDefinition schema = SchemaDefinition.builder()
                .name(request.getName())
                .dialect(request.getDialect())
                .project(project)
                .build();

        // Parse DDL script if provided
        if (request.getDdlScript() != null && !request.getDdlScript().isEmpty()) {
            parseDDL(request.getDdlScript(), schema);
        }

        schema = schemaRepository.save(schema);

        return mapToResponse(schema);
    }

    @Transactional
    public SchemaResponse getSchema(Long schemaId) {
        SchemaDefinition schema = schemaRepository.findByIdWithFullDetails(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));
        return mapToResponse(schema);
    }

    @Transactional
    public List<SchemaResponse> getSchemasByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        
        return schemaRepository.findByProject(project).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteSchema(Long schemaId, String username) {
        SchemaDefinition schema = schemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));

        if (!schema.getProject().getOwner().getFullName().equals(username)) {
            throw new BadRequestException("You don't have permission to delete this schema");
        }

        schemaRepository.delete(schema);
    }

    @Transactional
    public SchemaResponse addTable(Long schemaId, TableCreateRequest request, String username) {
        SchemaDefinition schema = schemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", schemaId));

        if (!schema.getProject().getOwner().getFullName().equals(username)) {
            throw new BadRequestException("You don't have permission to modify this schema");
        }

        if (tableRepository.existsBySchemaAndTableName(schema, request.getTableName())) {
            throw new BadRequestException("Table already exists in this schema");
        }

        TableDefinition table = TableDefinition.builder()
                .schema(schema)
                .tableName(request.getTableName())
                .estimatedRows(request.getEstimatedRows() != null ? request.getEstimatedRows() : 0L)
                .description(request.getDescription())
                .build();

        // Add columns
        if (request.getColumns() != null) {
            for (TableCreateRequest.ColumnRequest col : request.getColumns()) {
                ColumnDefinition column = ColumnDefinition.builder()
                        .table(table)
                        .columnName(col.getColumnName())
                        .dataType(col.getDataType())
                        .isNullable(col.getIsNullable() != null ? col.getIsNullable() : true)
                        .isPrimaryKey(col.getIsPrimaryKey() != null ? col.getIsPrimaryKey() : false)
                        .isForeignKey(col.getIsForeignKey() != null ? col.getIsForeignKey() : false)
                        .referencesTable(col.getReferencesTable())
                        .referencesColumn(col.getReferencesColumn())
                        .build();
                table.getColumns().add(column);
            }
        }

        // Add indexes
        if (request.getIndexes() != null) {
            for (TableCreateRequest.IndexRequest idx : request.getIndexes()) {
                IndexDefinition index = IndexDefinition.builder()
                        .table(table)
                        .indexName(idx.getIndexName())
                        .columns(idx.getColumns())
                        .isUnique(idx.getIsUnique() != null ? idx.getIsUnique() : false)
                        .indexType(idx.getIndexType() != null ? idx.getIndexType() : "BTREE")
                        .build();
                table.getIndexes().add(index);
            }
        }

        schema.getTables().add(table);
        schema = schemaRepository.save(schema);

        return mapToResponse(schema);
    }

    private void parseDDL(String ddlScript, SchemaDefinition schema) {
        // Simple DDL parser for CREATE TABLE statements
        Pattern createTablePattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w.]+)\\s*\\(([^;]+)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = createTablePattern.matcher(ddlScript);
        while (matcher.find()) {
            String tableName = matcher.group(1).replaceAll("\"", "").trim();
            String columnsStr = matcher.group(2);

            TableDefinition table = TableDefinition.builder()
                    .schema(schema)
                    .tableName(tableName)
                    .indexes(new ArrayList<>())
                    .build();

            // Parse columns
            String[] parts = columnsStr.split(",(?![^()]*\\))");
            for (String part : parts) {
                part = part.trim();
                if (part.toUpperCase().startsWith("PRIMARY KEY") ||
                    part.toUpperCase().startsWith("FOREIGN KEY") ||
                    part.toUpperCase().startsWith("CONSTRAINT") ||
                    part.toUpperCase().startsWith("INDEX") ||
                    part.toUpperCase().startsWith("UNIQUE")) {
                    continue;
                }

                String[] tokens = part.split("\\s+", 3);
                if (tokens.length >= 2) {
                    String colName = tokens[0].replaceAll("\"", "");
                    String dataType = tokens[1];

                    boolean isPK = part.toUpperCase().contains("PRIMARY KEY");
                    boolean notNull = part.toUpperCase().contains("NOT NULL");

                    ColumnDefinition column = ColumnDefinition.builder()
                            .table(table)
                            .columnName(colName)
                            .dataType(dataType)
                            .isPrimaryKey(isPK)
                            .isNullable(!notNull && !isPK)
                            .build();
                    table.getColumns().add(column);
                }
            }

            schema.getTables().add(table);
        }
    }

    private SchemaResponse mapToResponse(SchemaDefinition schema) {
        return SchemaResponse.builder()
                .id(schema.getId())
                .name(schema.getName())
                .dialect(schema.getDialect())
                .projectId(schema.getProject().getId())
                .tables(schema.getTables().stream()
                        .map(this::mapTableToResponse)
                        .toList())
                .createdAt(schema.getCreatedAt())
                .updatedAt(schema.getUpdatedAt())
                .build();
    }

    private SchemaResponse.TableResponse mapTableToResponse(TableDefinition table) {
        return SchemaResponse.TableResponse.builder()
                .id(table.getId())
                .tableName(table.getTableName())
                .estimatedRows(table.getEstimatedRows())
                .description(table.getDescription())
                .columns(table.getColumns().stream()
                        .map(col -> SchemaResponse.ColumnResponse.builder()
                                .id(col.getId())
                                .columnName(col.getColumnName())
                                .dataType(col.getDataType())
                                .isNullable(col.getIsNullable())
                                .isPrimaryKey(col.getIsPrimaryKey())
                                .isForeignKey(col.getIsForeignKey())
                                .referencesTable(col.getReferencesTable())
                                .referencesColumn(col.getReferencesColumn())
                                .build())
                        .toList())
                .indexes(table.getIndexes().stream()
                        .map(idx -> SchemaResponse.IndexResponse.builder()
                                .id(idx.getId())
                                .indexName(idx.getIndexName())
                                .columns(idx.getColumns())
                                .isUnique(idx.getIsUnique())
                                .indexType(idx.getIndexType())
                                .build())
                        .toList())
                .build();
    }

}
