package com.example.querysence.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.querysence.exception.BadRequestException;
import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.ConnectionStatus;
import com.example.querysence.model.DbConnection;
import com.example.querysence.model.DbDialect;
import com.example.querysence.model.Project;
import com.example.querysence.model.User;
import com.example.querysence.model.dto.DbConnectionDto;
import com.example.querysence.model.dto.DbConnectionRequest;
import com.example.querysence.model.dto.TestConnectionResponse;
import com.example.querysence.repository.DbConnectionRepository;
import com.example.querysence.repository.ProjectRepository;
import com.example.querysence.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DbConnectionService {

        private final ProjectRepository projectRepository;
        private final DbConnectionRepository dbConnectionRepository;
        private final DbConnectionCryptoService cryptoService;
        private final UserRepository userRepository;

        private static final int TEST_CONNECTION_TIMEOUT_SECONDS = 5;

        @Transactional
        public DbConnectionDto create(Long projectId, DbConnectionRequest request, String username) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

                if (dbConnectionRepository.existsByNameAndProject(request.getName(), project)) {
                        throw new BadRequestException("A connection with this name already exists in the project");
                }

                DbConnection connection = DbConnection.builder()
                                .project(project)
                                .name(request.getName())
                                .host(request.getHost())
                                .port(request.getPort())
                                .databaseName(request.getDatabaseName())
                                .username(request.getUsername())
                                .encryptedPassword(cryptoService.encrypt(request.getPassword()))
                                .dialect(DbDialect.valueOf(request.getDialect()))
                                .sslEnabled(request.getSslEnabled())
                                .readOnlyEnforced(request.getReadOnlyEnforced())
                                .status(ConnectionStatus.UNTESTED)
                                .build();

                connection = dbConnectionRepository.save(connection);
                log.info("Created DB connection '{}' for project {}", connection.getName(), projectId);

                return mapToDto(connection);
        }

        @Transactional(readOnly = true)
        public List<DbConnectionDto> listByProject(Long projectId, String keycloakUserId) {
                User user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Project project = projectRepository.findByIdAndOwner(projectId, user)
                                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
                return dbConnectionRepository.findByProjectId(projectId).stream()
                                .map(this::mapToDto)
                                .toList();
        }

        @Transactional
        public void delete(Long projectId, Long connectionId, String keycloakUserId) {
                User user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Project project = projectRepository.findByIdAndOwner(projectId, user)
                                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
                DbConnection connection = dbConnectionRepository.findByIdAndProjectId(connectionId, projectId)
                                .orElseThrow(() -> new ResourceNotFoundException("DbConnection", "id", connectionId));

                dbConnectionRepository.delete(connection);
        }

        @Transactional
        public TestConnectionResponse testConnection(Long projectId, Long connectionId, String keycloakUserId) {
                User user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Project project = projectRepository.findByIdAndOwner(projectId, user)
                                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

                DbConnection connection = dbConnectionRepository.findByIdAndProjectId(connectionId, projectId)
                                .orElseThrow(() -> new ResourceNotFoundException("DbConnection", "id", connectionId));

                long start = System.currentTimeMillis();
                TestConnectionResponse response;

                try (Connection ignored = openConnection(connection)) {
                        long latency = System.currentTimeMillis() - start;
                        connection.setStatus(ConnectionStatus.CONNECTED);
                        response = TestConnectionResponse.builder()
                                        .success(true)
                                        .status(ConnectionStatus.CONNECTED.name())
                                        .message("Connected successfully")
                                        .latencyMs(latency)
                                        .build();
                } catch (SQLException e) {
                        log.error("Database connection failed", e);
                        boolean isAuthFailure = e.getSQLState() != null && e.getSQLState().startsWith("28");
                        ConnectionStatus failedStatus = isAuthFailure
                                        ? ConnectionStatus.EXPIRED_CREDENTIALS
                                        : ConnectionStatus.FAILED;
                        connection.setStatus(failedStatus);
                        response = TestConnectionResponse.builder()
                                        .success(false)
                                        .status(failedStatus.name())
                                        .message(e.getMessage())
                                        .latencyMs(System.currentTimeMillis() - start)
                                        .build();
                }

                connection.setLastTestedAt(java.time.LocalDateTime.now());
                dbConnectionRepository.save(connection);

                return response;
        }

        Connection openConnection(DbConnection connection) throws SQLException {
                String url = buildJdbcUrl(connection);
                Properties props = new Properties();
                props.setProperty("user", connection.getUsername());
                props.setProperty("password", cryptoService.decrypt(connection.getEncryptedPassword()));
                props.setProperty("connectTimeout", String.valueOf(TEST_CONNECTION_TIMEOUT_SECONDS * 1000));

                if (Boolean.TRUE.equals(connection.getSslEnabled())) {
                        props.setProperty("ssl", "true");
                }

                DriverManager.setLoginTimeout(TEST_CONNECTION_TIMEOUT_SECONDS);
                return DriverManager.getConnection(url, props);
        }

        private String buildJdbcUrl(DbConnection connection) {
                return switch (connection.getDialect()) {
                        case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s",
                                        connection.getHost(), connection.getPort(), connection.getDatabaseName());
                        case MYSQL -> String.format("jdbc:mysql://%s:%d/%s",
                                        connection.getHost(), connection.getPort(), connection.getDatabaseName());
                        case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
                                        connection.getHost(), connection.getPort(), connection.getDatabaseName());
                        case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s",
                                        connection.getHost(), connection.getPort(), connection.getDatabaseName());
                };
        }

        private DbConnectionDto mapToDto(DbConnection connection) {
                return DbConnectionDto.builder()
                                .id(connection.getId())
                                .projectId(connection.getProject().getId())
                                .name(connection.getName())
                                .host(connection.getHost())
                                .port(connection.getPort())
                                .databaseName(connection.getDatabaseName())
                                .username(connection.getUsername())
                                .dialect(connection.getDialect().name())
                                .sslEnabled(connection.getSslEnabled())
                                .readOnlyEnforced(connection.getReadOnlyEnforced())
                                .status(connection.getStatus().name())
                                .lastTestedAt(connection.getLastTestedAt())
                                .lastSyncedAt(connection.getLastSyncedAt())
                                .createdAt(connection.getCreatedAt())
                                .updatedAt(connection.getUpdatedAt())
                                .build();
        }
}
