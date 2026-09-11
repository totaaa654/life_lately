package com.lifelately.database;

import com.lifelately.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private final DatabaseConfig config;

    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.url(), config.username(), config.password());
    }

    public Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(config.serverUrl(), config.username(), config.password());
    }

    public boolean isAvailable() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }
}
