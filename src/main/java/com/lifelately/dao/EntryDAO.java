package com.lifelately.dao;

import com.lifelately.database.DatabaseConnection;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.model.Tag;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EntryDAO {
    private static final String SELECT_COLUMNS = """
            SELECT e.id, e.title, e.content, e.entry_date, e.created_at, e.updated_at,
                   e.is_favorite, e.deleted_at, m.id AS mood_id, m.name AS mood_name,
                   m.icon AS mood_icon, m.color_hex AS mood_color, m.sort_order AS mood_order
            FROM entries e JOIN moods m ON m.id = e.mood_id
            """;

    private final DatabaseConnection databaseConnection;

    public EntryDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<Entry> findAll() {
        return search("", null, null, false, "NEWEST");
    }

    public Optional<Entry> findById(long id) {
        String sql = SELECT_COLUMNS + " WHERE e.id = ? AND e.deleted_at IS NULL";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                Entry entry = map(result);
                entry.setTags(findTags(connection, entry.getId()));
                return Optional.of(entry);
            }
        } catch (SQLException exception) {
            throw databaseError("load entry", exception);
        }
    }

    public List<Entry> search(String query, Long moodId, Long tagId, boolean favoritesOnly, String sort) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        List<Object> parameters = new ArrayList<>();
        if (tagId != null) sql.append(" JOIN entry_tags filter_et ON filter_et.entry_id = e.id ");
        sql.append(" WHERE e.deleted_at IS NULL ");
        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(e.title) LIKE ? OR LOWER(e.content) LIKE ?) ");
            String searchValue = "%" + query.toLowerCase().trim() + "%";
            parameters.add(searchValue);
            parameters.add(searchValue);
        }
        if (moodId != null) {
            sql.append(" AND e.mood_id = ? ");
            parameters.add(moodId);
        }
        if (tagId != null) {
            sql.append(" AND filter_et.tag_id = ? ");
            parameters.add(tagId);
        }
        if (favoritesOnly) sql.append(" AND e.is_favorite = TRUE ");
        sql.append(switch (sort == null ? "NEWEST" : sort) {
            case "OLDEST" -> " ORDER BY e.entry_date ASC, e.created_at ASC";
            case "EDITED" -> " ORDER BY e.updated_at DESC";
            default -> " ORDER BY e.entry_date DESC, e.created_at DESC";
        });

        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
            List<Entry> entries = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) entries.add(map(result));
            }
            for (Entry entry : entries) entry.setTags(findTags(connection, entry.getId()));
            return entries;
        } catch (SQLException exception) {
            throw databaseError("search entries", exception);
        }
    }

    public Entry insert(Entry entry) {
        String sql = """
                INSERT INTO entries (title, content, mood_id, entry_date, is_favorite)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindEntry(statement, entry);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No key returned for new entry");
                    entry.setId(keys.getLong(1));
                }
                replaceTags(connection, entry.getId(), entry.getTags());
                connection.commit();
                return findById(entry.getId()).orElseThrow();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseError("save entry", exception);
        }
    }

    public Entry update(Entry entry) {
        String sql = """
                UPDATE entries SET title = ?, content = ?, mood_id = ?, entry_date = ?,
                    is_favorite = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL
                """;
        try (var connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(sql)) {
                bindEntry(statement, entry);
                statement.setLong(6, entry.getId());
                if (statement.executeUpdate() == 0) throw new SQLException("Entry was not found");
                replaceTags(connection, entry.getId(), entry.getTags());
                connection.commit();
                return findById(entry.getId()).orElseThrow();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseError("update entry", exception);
        }
    }

    public void softDelete(long id) {
        changeDeletedAt(id, "CURRENT_TIMESTAMP");
    }

    public void restore(long id) {
        changeDeletedAt(id, "NULL");
    }

    private void changeDeletedAt(long id, String value) {
        String sql = "UPDATE entries SET deleted_at = " + value + " WHERE id = ?";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("update entry deletion status", exception);
        }
    }

    private void bindEntry(java.sql.PreparedStatement statement, Entry entry) throws SQLException {
        statement.setString(1, entry.getTitle());
        statement.setString(2, entry.getContent());
        statement.setLong(3, entry.getMood().id());
        statement.setDate(4, Date.valueOf(entry.getEntryDate()));
        statement.setBoolean(5, entry.isFavorite());
    }

    private Entry map(ResultSet result) throws SQLException {
        Entry entry = new Entry();
        entry.setId(result.getLong("id"));
        entry.setTitle(result.getString("title"));
        entry.setContent(result.getString("content"));
        entry.setEntryDate(result.getDate("entry_date").toLocalDate());
        entry.setCreatedAt(result.getTimestamp("created_at").toLocalDateTime());
        entry.setUpdatedAt(result.getTimestamp("updated_at").toLocalDateTime());
        entry.setFavorite(result.getBoolean("is_favorite"));
        Timestamp deleted = result.getTimestamp("deleted_at");
        entry.setDeletedAt(deleted == null ? null : deleted.toLocalDateTime());
        entry.setMood(new Mood(result.getLong("mood_id"), result.getString("mood_name"),
                result.getString("mood_icon"), result.getString("mood_color"), result.getInt("mood_order")));
        return entry;
    }

    private List<Tag> findTags(java.sql.Connection connection, long entryId) throws SQLException {
        String sql = """
                SELECT t.id, t.name, t.color_hex FROM tags t
                JOIN entry_tags et ON et.tag_id = t.id WHERE et.entry_id = ? ORDER BY t.name
                """;
        List<Tag> tags = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entryId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tags.add(new Tag(result.getLong("id"), result.getString("name"), result.getString("color_hex")));
                }
            }
        }
        return tags;
    }

    private void replaceTags(java.sql.Connection connection, long entryId, List<Tag> tags) throws SQLException {
        try (var delete = connection.prepareStatement("DELETE FROM entry_tags WHERE entry_id = ?")) {
            delete.setLong(1, entryId);
            delete.executeUpdate();
        }
        try (var insert = connection.prepareStatement("INSERT INTO entry_tags (entry_id, tag_id) VALUES (?, ?)")) {
            for (Tag tag : tags) {
                insert.setLong(1, entryId);
                insert.setLong(2, tag.id());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private IllegalStateException databaseError(String action, SQLException cause) {
        return new IllegalStateException("Could not " + action + ". Is Laragon MySQL running?", cause);
    }
}
