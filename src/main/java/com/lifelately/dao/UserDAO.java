package com.lifelately.dao;

import com.lifelately.database.DatabaseConnection;
import com.lifelately.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public final class UserDAO {
    private final DatabaseConnection databaseConnection;

    public UserDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public boolean hasUsers() {
        String sql = "SELECT EXISTS(SELECT 1 FROM users LIMIT 1)";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            return result.next() && result.getBoolean(1);
        } catch (SQLException exception) {
            throw databaseError("check local accounts", exception);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT id, username, display_name, password_hash, password_salt, created_at, last_login_at
                FROM users WHERE LOWER(username) = LOWER(?)
                """;
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("load local account", exception);
        }
    }

    public Optional<User> findById(long userId) {
        String sql = """
                SELECT id, username, display_name, password_hash, password_salt, created_at, last_login_at
                FROM users WHERE id = ?
                """;
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("restore local account", exception);
        }
    }

    public Optional<User> findRememberedUser() {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = 'remembered_user_id'";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            if (!result.next()) return Optional.empty();
            try {
                return findById(Long.parseLong(result.getString(1)));
            } catch (NumberFormatException invalidSetting) {
                clearRememberedUser();
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("restore local session", exception);
        }
    }

    public void rememberUser(long userId) {
        String sql = """
                INSERT INTO app_settings (setting_key, setting_value) VALUES ('remembered_user_id', ?)
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
                """;
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, Long.toString(userId));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("remember local session", exception);
        }
    }

    public void clearRememberedUser() {
        String sql = "DELETE FROM app_settings WHERE setting_key = 'remembered_user_id'";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("clear local session", exception);
        }
    }

    public User insert(String username, String displayName, String passwordHash, String passwordSalt) {
        String sql = """
                INSERT INTO users (username, display_name, password_hash, password_salt)
                VALUES (?, ?, ?, ?)
                """;
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, displayName);
            statement.setString(3, passwordHash);
            statement.setString(4, passwordSalt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No key returned for local account");
                return findByUsername(username).orElseThrow();
            }
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062) {
                throw new IllegalArgumentException("That username is already taken.", exception);
            }
            throw databaseError("create local account", exception);
        }
    }

    public void claimUnownedEntries(long userId) {
        String sql = "UPDATE entries SET user_id = ? WHERE user_id IS NULL";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("assign existing journal entries", exception);
        }
    }

    public void recordLogin(long userId) {
        String sql = "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("record login", exception);
        }
    }

    public User updateDisplayName(long userId, String displayName) {
        String sql = "UPDATE users SET display_name = ? WHERE id = ?";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, displayName);
            statement.setLong(2, userId);
            if (statement.executeUpdate() == 0) throw new SQLException("User was not found");
            return findById(userId).orElseThrow();
        } catch (SQLException exception) {
            throw databaseError("update display name", exception);
        }
    }

    public User updatePassword(long userId, String passwordHash, String passwordSalt) {
        String sql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, passwordSalt);
            statement.setLong(3, userId);
            if (statement.executeUpdate() == 0) throw new SQLException("User was not found");
            return findById(userId).orElseThrow();
        } catch (SQLException exception) {
            throw databaseError("update password", exception);
        }
    }

    private User map(ResultSet result) throws SQLException {
        Timestamp lastLogin = result.getTimestamp("last_login_at");
        return new User(
                result.getLong("id"),
                result.getString("username"),
                result.getString("display_name"),
                result.getString("password_hash"),
                result.getString("password_salt"),
                result.getTimestamp("created_at").toLocalDateTime(),
                lastLogin == null ? null : lastLogin.toLocalDateTime()
        );
    }

    private IllegalStateException databaseError(String action, SQLException cause) {
        return new IllegalStateException("Could not " + action + ". Is Laragon MySQL running?", cause);
    }
}
