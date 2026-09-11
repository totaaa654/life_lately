package com.lifelately.dao;

import com.lifelately.database.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsDAO {
    private final DatabaseConnection databaseConnection;

    public SettingsDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Map<String, String> findAll() {
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        Map<String, String> settings = new LinkedHashMap<>();
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) settings.put(result.getString("setting_key"), result.getString("setting_value"));
            return settings;
        } catch (SQLException exception) {
            throw databaseError("load settings", exception);
        }
    }

    public void save(String key, String value) {
        String sql = """
                INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
                """;
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("save settings", exception);
        }
    }

    private IllegalStateException databaseError(String action, SQLException cause) {
        return new IllegalStateException("Could not " + action + ". Is Laragon MySQL running?", cause);
    }
}
