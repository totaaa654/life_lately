package com.lifelately.dao;

import com.lifelately.database.DatabaseConnection;
import com.lifelately.model.Tag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class TagDAO {
    private static final String DEFAULT_COLOR = "#8B7FD6";
    private final DatabaseConnection databaseConnection;

    public TagDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<Tag> findAllForUser(long userId) {
        String sql = """
                SELECT DISTINCT t.id, t.name, t.color_hex
                FROM tags t
                JOIN entry_tags et ON et.tag_id = t.id
                JOIN entries e ON e.id = et.entry_id
                WHERE e.user_id = ? AND e.deleted_at IS NULL
                ORDER BY t.name
                """;
        List<Tag> tags = new ArrayList<>();
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) tags.add(map(result));
                return tags;
            }
        } catch (SQLException exception) {
            throw databaseError("load tags", exception);
        }
    }

    public Tag findOrCreate(String name) {
        String selectSql = "SELECT id, name, color_hex FROM tags WHERE name = ?";
        String insertSql = "INSERT INTO tags (name, color_hex) VALUES (?, ?)";
        try (var connection = databaseConnection.getConnection();
             var select = connection.prepareStatement(selectSql)) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) return map(result);
            }

            try (var insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, name);
                insert.setString(2, DEFAULT_COLOR);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) return new Tag(keys.getLong(1), name, DEFAULT_COLOR);
                }
            }
            throw new SQLException("No key returned for new tag");
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062) return findOrCreate(name);
            throw databaseError("save tag", exception);
        }
    }

    private Tag map(ResultSet result) throws SQLException {
        return new Tag(result.getLong("id"), result.getString("name"), result.getString("color_hex"));
    }

    private IllegalStateException databaseError(String action, SQLException cause) {
        return new IllegalStateException("Could not " + action + ". Is Laragon MySQL running?", cause);
    }
}
