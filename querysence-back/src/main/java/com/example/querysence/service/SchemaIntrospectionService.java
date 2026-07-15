package com.example.querysence.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.querysence.exception.ResourceNotFoundException;
import com.example.querysence.model.ColumnDefinition;
import com.example.querysence.model.DbConnection;
import com.example.querysence.model.SchemaDefinition;
import com.example.querysence.model.SchemaSource;
import com.example.querysence.model.SchemaSyncLog;
import com.example.querysence.model.SyncStatus;
import com.example.querysence.model.TableDefinition;
import com.example.querysence.model.dto.SchemaSyncResponse;
import com.example.querysence.repository.DbConnectionRepository;
import com.example.querysence.repository.SchemaDefinitionRepository;
import com.example.querysence.repository.SchemaSyncLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaIntrospectionService {

    @Autowired
    private DbConnectionRepository dbConnectionRepository;
    @Autowired
    private SchemaDefinitionRepository schemaRepository;
    @Autowired
    private SchemaSyncLogRepository syncLogRepository;
    @Autowired
    private DbConnectionService dbConnectionService;

 
    @Transactional
    public SchemaSyncResponse syncSchema(Long projectId, Long dbConnectionId, Long existingSchemaId, String username) {
        DbConnection connection = dbConnectionRepository.findByIdAndProjectId(dbConnectionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("DbConnection", "id", dbConnectionId));

        SchemaSyncLog syncLog = SchemaSyncLog.builder()
                .dbConnection(connection)
                .status(SyncStatus.RUNNING)
                .build();
        syncLog = syncLogRepository.save(syncLog);

        SchemaDefinition schema;
        try {
            schema = existingSchemaId != null
                    ? schemaRepository.findById(existingSchemaId)
                            .orElseThrow(() -> new ResourceNotFoundException("Schema", "id", existingSchemaId))
                    : SchemaDefinition.builder()
                            .project(connection.getProject())
                            .name(connection.getName() + " (synced)")
                            .dialect(connection.getDialect().name())
                            .source(SchemaSource.SYNCED)
                            .dbConnection(connection)
                            .build();

            schema.setSource(SchemaSource.SYNCED);
            schema.setDbConnection(connection);

            int[] counts = introspectInto(connection, schema);

            schema = schemaRepository.save(schema);

            connection.setLastSyncedAt(LocalDateTime.now());
            dbConnectionRepository.save(connection);

            syncLog.setStatus(SyncStatus.SUCCESS);
            syncLog.setTablesDiscovered(counts[0]);
            syncLog.setColumnsDiscovered(counts[1]);
            syncLog.setIndexesDiscovered(counts[2]);
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLogRepository.save(syncLog);

            log.info("Schema sync succeeded for connection {}: {} tables, {} columns, {} indexes",
                    dbConnectionId, counts[0], counts[1], counts[2]);

            return SchemaSyncResponse.builder()
                    .syncLogId(syncLog.getId())
                    .schemaId(schema.getId())
                    .status(SyncStatus.SUCCESS.name())
                    .tablesDiscovered(counts[0])
                    .columnsDiscovered(counts[1])
                    .indexesDiscovered(counts[2])
                    .startedAt(syncLog.getStartedAt())
                    .finishedAt(syncLog.getFinishedAt())
                    .build();

        } catch (SQLException e) {
            log.error("Schema sync failed for connection {}: {}", dbConnectionId, e.getMessage());
            syncLog.setStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLogRepository.save(syncLog);

            return SchemaSyncResponse.builder()
                    .syncLogId(syncLog.getId())
                    .status(SyncStatus.FAILED.name())
                    .errorMessage(e.getMessage())
                    .startedAt(syncLog.getStartedAt())
                    .finishedAt(syncLog.getFinishedAt())
                    .build();
        }
    }


    private int[] introspectInto(DbConnection connection, SchemaDefinition schema) throws SQLException {
        List<TableDefinition> tables = new ArrayList<>();
        int columnCount = 0;
        int indexCount = 0;

        try (Connection conn = dbConnectionService.openConnection(connection)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schemaPattern = conn.getSchema(); // e.g. "public" for Postgres

            List<String> tableNames = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }

            for (String tableName : tableNames) {
                TableDefinition table = TableDefinition.builder()
                        .schema(schema)
                        .tableName(tableName)
                        .estimatedRows(0L)
                        .build();

                Set<String> primaryKeyColumns = new HashSet<>();
                try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schemaPattern, tableName)) {
                    while (pkRs.next()) {
                        primaryKeyColumns.add(pkRs.getString("COLUMN_NAME"));
                    }
                }

                try (ResultSet colRs = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
                    while (colRs.next()) {
                        String columnName = colRs.getString("COLUMN_NAME");
                        ColumnDefinition column = ColumnDefinition.builder()
                                .table(table)
                                .columnName(columnName)
                                .dataType(colRs.getString("TYPE_NAME"))
                                .isNullable(colRs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls)
                                .isPrimaryKey(primaryKeyColumns.contains(columnName))
                                .isForeignKey(false)
                                .build();
                        table.getColumns().add(column);
                        columnCount++;
                    }
                }

                // Foreign keys: mark referencing columns and record the target table/column
                try (ResultSet fkRs = metaData.getImportedKeys(catalog, schemaPattern, tableName)) {
                    while (fkRs.next()) {
                        String fkColumnName = fkRs.getString("FKCOLUMN_NAME");
                        String pkTableName = fkRs.getString("PKTABLE_NAME");
                        String pkColumnName = fkRs.getString("PKCOLUMN_NAME");
                        table.getColumns().stream()
                                .filter(c -> c.getColumnName().equals(fkColumnName))
                                .findFirst()
                                .ifPresent(c -> {
                                    c.setIsForeignKey(true);
                                    c.setReferencesTable(pkTableName);
                                    c.setReferencesColumn(pkColumnName);
                                });
                    }
                }

                try (ResultSet idxRs = metaData.getIndexInfo(catalog, schemaPattern, tableName, false, false)) {
                    while (idxRs.next()) {
                        String indexName = idxRs.getString("INDEX_NAME");
                        if (indexName == null) {
                            continue; // null entries represent table statistics, not real indexes
                        }
                        String columnName = idxRs.getString("COLUMN_NAME");
                        boolean nonUnique = idxRs.getBoolean("NON_UNIQUE");

                        table.getIndexes().stream()
                                .filter(i -> i.getIndexName().equals(indexName))
                                .findFirst()
                                .ifPresentOrElse(
                                        existing -> existing.getColumns().add(columnName),
                                        () -> {
                                            com.example.querysence.model.IndexDefinition newIndex =
                                                    com.example.querysence.model.IndexDefinition.builder()
                                                            .table(table)
                                                            .indexName(indexName)
                                                            .columns(new ArrayList<>(List.of(columnName)))
                                                            .isUnique(!nonUnique)
                                                            .indexType("BTREE")
                                                            .build();
                                            table.getIndexes().add(newIndex);
                                        });
                    }
                }
                indexCount += table.getIndexes().size();

                tables.add(table);
            }
        }

        schema.getTables().clear();
        schema.getTables().addAll(tables);

        return new int[]{tables.size(), columnCount, indexCount};
    }
}
