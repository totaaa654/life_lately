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
            throw databaseError("create local account", exception);
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
