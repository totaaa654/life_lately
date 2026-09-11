package com.lifelately.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private static final String DATABASE_NAME = "life_lately";
    private final DatabaseConnection databaseConnection;

    public DatabaseInitializer(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public void initialize() throws SQLException {
        try (Connection connection = databaseConnection.getServerConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        executeResource("/com/lifelately/database/schema.sql");
        executeResource("/com/lifelately/database/seed.sql");
        migrateExistingData();
    }

    private void migrateExistingData() throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "entries", "user_id")) {
                statement.executeUpdate("ALTER TABLE entries ADD COLUMN user_id BIGINT UNSIGNED NULL AFTER id");
            }

            statement.executeUpdate("""
                    UPDATE entries
                    SET user_id = (SELECT id FROM users ORDER BY id LIMIT 1)
                    WHERE user_id IS NULL AND EXISTS (SELECT 1 FROM users)
                    """);

            if (!indexExists(connection, "entries", "idx_entries_user")) {
                statement.executeUpdate("CREATE INDEX idx_entries_user ON entries(user_id)");
            }
            if (!foreignKeyExists(connection, "entries", "fk_entries_user")) {
                statement.executeUpdate("""
                        ALTER TABLE entries ADD CONSTRAINT fk_entries_user
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        """);
            }

            statement.executeUpdate("""
                    INSERT INTO user_settings (user_id, setting_key, setting_value)
                    SELECT u.id, s.setting_key, s.setting_value
                    FROM users u CROSS JOIN app_settings s
                    WHERE s.setting_key IN ('theme', 'accent_color', 'date_format', 'first_day_of_week')
                    ON DUPLICATE KEY UPDATE setting_value = user_settings.setting_value
                    """);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (var result = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
        }
    }

    private boolean indexExists(Connection connection, String table, String index) throws SQLException {
        try (var result = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (result.next()) {
                if (index.equalsIgnoreCase(result.getString("INDEX_NAME"))) return true;
            }
            return false;
        }
    }

    private boolean foreignKeyExists(Connection connection, String table, String foreignKey) throws SQLException {
        try (var result = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, table)) {
            while (result.next()) {
                if (foreignKey.equalsIgnoreCase(result.getString("FK_NAME"))) return true;
            }
            return false;
        }
    }

    private void executeResource(String resourcePath) throws SQLException {
        String sql = readResource(resourcePath);
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            for (String command : sql.split(";")) {
                String cleaned = command.lines()
                        .filter(line -> !line.stripLeading().startsWith("--"))
                        .reduce("", (left, right) -> left + System.lineSeparator() + right)
                        .trim();
                if (!cleaned.isBlank()) {
                    statement.execute(cleaned);
                }
            }
        }
    }

    private String readResource(String path) {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing database resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read database resource: " + path, exception);
        }
    }
}
