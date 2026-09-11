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
