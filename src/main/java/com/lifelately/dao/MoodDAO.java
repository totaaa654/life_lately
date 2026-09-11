package com.lifelately.dao;

import com.lifelately.database.DatabaseConnection;
import com.lifelately.model.Mood;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MoodDAO {
    private final DatabaseConnection databaseConnection;

    public MoodDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<Mood> findAll() {
        String sql = "SELECT id, name, icon, color_hex, sort_order FROM moods ORDER BY sort_order";
        List<Mood> moods = new ArrayList<>();
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                moods.add(map(result));
            }
            return moods;
        } catch (SQLException exception) {
            throw databaseError("load moods", exception);
        }
    }

    public Mood findById(long id) {
        String sql = "SELECT id, name, icon, color_hex, sort_order FROM moods WHERE id = ?";
        try (var connection = databaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseError("load mood", exception);
        }
    }

    private Mood map(ResultSet result) throws SQLException {
        return new Mood(result.getLong("id"), result.getString("name"), result.getString("icon"),
                result.getString("color_hex"), result.getInt("sort_order"));
    }

    private IllegalStateException databaseError(String action, SQLException cause) {
        return new IllegalStateException("Could not " + action + ". Is Laragon MySQL running?", cause);
    }
}
