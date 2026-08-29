package com.example.querysence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.querysence.exception.BadRequestException;
import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.ColumnDefinition;
import com.example.querysence.model.DbConnection;
import com.example.querysence.model.IndexDefinition;
import com.example.querysence.model.Project;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.TableDefinition;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.DbConnectionDto;
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
        private final ProjectRepository projectRepository;
        private final UserRepository userRepository;
        private final SchemaDefinitionRepository schemaRepository;
        private final TableDefinitionRepository tableRepository;

        @Transactional
        public ProjectResponse create(ProjectCreateRequest request, String keycloakUserId) {
                User user = userRepository.findByKeycloakUserId(keycloakUserId)
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
        public List<ProjectResponse> listByUser(String keycloakUserId) {
                User user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                return projectRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional
        public ProjectResponse getById(
                        Long id,
                        String keycloakUserId) {

                Project project = projectRepository.findByIdWithSchemas(id);

                if (project == null) {
                        throw new ResourceNotFoundException(
                                        "Project",
                                        "id",
                                        id);
                }

                User user = userRepository
                                .findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                if (!project.getOwner().getId().equals(user.getId())) {
                        throw new BadRequestException(
                                        "You don't have permission to access this project");
                }

                return mapToResponseWithSchemas(project);
        }

        @Transactional
        public void delete(Long id, String keycloakUserId) {
                Project project = projectRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

                User user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                if (!project.getOwner().getId().equals(user.getId())) {
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
                                .dbConnections(mapConnectionsToDto(project))
                                .createdAt(project.getCreatedAt())
                                .updatedAt(project.getUpdatedAt())
                                .build();
        }

        private ProjectResponse mapToResponseWithSchemas(Project project) {
                List<SchemaResponse> schemas = project.getSchemas().stream()
                                .map(this::mapToResponse)
                                .toList();

                return ProjectResponse.builder()
                                .id(project.getId())
                                .name(project.getName())
                                .description(project.getDescription())
                                .schemaCount(schemas.size())
                                .schemas(schemas)
                                .dbConnections(mapConnectionsToDto(project))
                                .createdAt(project.getCreatedAt())
                                .updatedAt(project.getUpdatedAt())
                                .build();
        }

        private List<DbConnectionDto> mapConnectionsToDto(Project project) {
                List<DbConnection> connections = project.getDbConnections();
                if (connections == null) {
                        return new ArrayList<>();
                }
                return connections.stream()
                                .map(c -> DbConnectionDto.builder()
                                                .id(c.getId())
                                                .projectId(project.getId())
                                                .name(c.getName())
                                                .host(c.getHost())
                                                .port(c.getPort())
                                                .databaseName(c.getDatabaseName())
                                                .username(c.getUsername())
                                                .dialect(c.getDialect().name())
                                                .sslEnabled(c.getSslEnabled())
                                                .readOnlyEnforced(c.getReadOnlyEnforced())
                                                .status(c.getStatus().name())
                                                .lastTestedAt(c.getLastTestedAt())
                                                .lastSyncedAt(c.getLastSyncedAt())
                                                .createdAt(c.getCreatedAt())
                                                .updatedAt(c.getUpdatedAt())
                                                .build())
                                .toList();
        }

        @Transactional
        public SchemaResponse createSchema(
                        Long projectId,
                        SchemaCreateRequest request,
                        String keycloakUserId) {

                User user = userRepository
                                .findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Project project = projectRepository
                                .findByIdAndOwner(projectId, user)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project",
                                                "id",
                                                projectId));

                if (schemaRepository.existsByNameAndProject(
                                request.getName(), project)) {

                        throw new BadRequestException(
                                        "Schema with this name already exists in the project");
                }

                SchemaDefinition schema = SchemaDefinition.builder()
                                .name(request.getName())
                                .dialect(request.getDialect())
                                .project(project)
                                .build();

                if (request.getDdlScript() != null
                                && !request.getDdlScript().isEmpty()) {

                        parseDDL(request.getDdlScript(), schema);
                }

                schema = schemaRepository.save(schema);

                return mapToResponse(schema);
        }

        @Transactional
        public SchemaResponse getSchema(
                        Long schemaId,
                        String keycloakUserId) {

                User user = userRepository
                                .findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                SchemaDefinition schema = schemaRepository
                                .findByIdWithTables(schemaId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Schema",
                                                "id",
                                                schemaId));

                Project project = schema.getProject();

                if (!project.getOwner().getId().equals(user.getId())) {
                        throw new BadRequestException(
                                        "You don't have permission to access this schema");
                }

                return mapToResponse(schema);
        }

        @Transactional
        public List<SchemaResponse> getSchemasByProject(Long projectId, String keycloakUserId) {
                User user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Project project = projectRepository.findByIdAndOwner(projectId, user)
                                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

                return schemaRepository.findByProject(project).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional
        public void deleteSchema(
                        Long schemaId,
                        String keycloakUserId) {

                User user = userRepository
                                .findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                SchemaDefinition schema = schemaRepository
                                .findById(schemaId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Schema",
                                                "id",
                                                schemaId));

                Project project = schema.getProject();

                if (!project.getOwner().getId().equals(user.getId())) {
                        throw new BadRequestException(
                                        "You don't have permission to delete this schema");
                }

                schemaRepository.delete(schema);
        }

        @Transactional
        public SchemaResponse addTable(
                        Long schemaId,
                        TableCreateRequest request,
                        String keycloakUserId) {

                User user = userRepository
                                .findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                SchemaDefinition schema = schemaRepository
                                .findById(schemaId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Schema",
                                                "id",
                                                schemaId));

                Project project = schema.getProject();

                if (!project.getOwner().getId().equals(user.getId())) {
                        throw new BadRequestException(
                                        "You don't have permission to modify this schema");
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

                if (request.getColumns() != null) {
                        for (TableCreateRequest.ColumnRequest col : request.getColumns()) {
                                ColumnDefinition column = ColumnDefinition.builder()
                                                .table(table)
                                                .columnName(col.getColumnName())
                                                .dataType(col.getDataType())
                                                .isNullable(java.util.Objects.requireNonNullElse(col.getIsNullable(), true))
                                                .isPrimaryKey(java.util.Objects.requireNonNullElse(col.getIsPrimaryKey(), false))
                                                .isForeignKey(java.util.Objects.requireNonNullElse(col.getIsForeignKey(), false))
                                                .referencesTable(col.getReferencesTable())
                                                .referencesColumn(col.getReferencesColumn())
                                                .build();
                                table.getColumns().add(column);
                        }
                }

                if (request.getIndexes() != null) {
                        for (TableCreateRequest.IndexRequest idx : request.getIndexes()) {
                                IndexDefinition index = IndexDefinition.builder()
                                                .table(table)
                                                .indexName(idx.getIndexName())
                                                .columns(idx.getColumns())
                                                .isUnique(java.util.Objects.requireNonNullElse(idx.getIsUnique(), false))
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
                Pattern createTablePattern = Pattern.compile(
                                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w.]+)\\s*\\(([^;]+)\\)",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

                Matcher matcher = createTablePattern.matcher(ddlScript);
                while (matcher.find()) {
                        String tableName = matcher.group(1).replaceAll("\"", "").trim();
                        String columnsStr = matcher.group(2);

                        TableDefinition table = TableDefinition.builder()
                                        .schema(schema)
                                        .tableName(tableName)
                                        .indexes(new ArrayList<>())
                                        .build();

                        String[] parts = columnsStr.split(",(?![^()]*\\))");
                        for (String part : parts) {
                                part = part.trim();
                                if (part.toUpperCase(Locale.ROOT).startsWith("PRIMARY KEY") ||
                                                part.toUpperCase(Locale.ROOT).startsWith("FOREIGN KEY") ||
                                                part.toUpperCase(Locale.ROOT).startsWith("CONSTRAINT") ||
                                                part.toUpperCase(Locale.ROOT).startsWith("INDEX") ||
                                                part.toUpperCase(Locale.ROOT).startsWith("UNIQUE")) {
                                        continue;
                                }

                                String[] tokens = part.split("\\s+", 3);
                                if (tokens.length >= 2) {
                                        String colName = tokens[0].replaceAll("\"", "");
                                        String dataType = tokens[1];

                                        boolean isPK = part.toUpperCase(Locale.ROOT).contains("PRIMARY KEY");
                                        boolean notNull = part.toUpperCase(Locale.ROOT).contains("NOT NULL");

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
                                .source(schema.getSource() != null ? schema.getSource().name() : "MANUAL")
                                .dbConnectionId(schema.getDbConnection() != null ? schema.getDbConnection().getId()
                                                : null)
                                .lastSyncedAt(schema.getDbConnection() != null
                                                ? schema.getDbConnection().getLastSyncedAt()
                                                : null)
                                .tables(schema.getTables().stream()
                                                .map(this::mapTableToResponse)
                                                .toList())
                                .createdAt(schema.getCreatedAt())
                                .updatedAt(schema.getUpdatedAt())
                                .build();
        }

        @SuppressWarnings("PMD.UnusedPrivateMethod")
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