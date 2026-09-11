package com.lifelately;

import com.lifelately.config.AppConfig;
import com.lifelately.config.DatabaseConfig;
import com.lifelately.database.DatabaseConnection;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseOneIntegrationTest {
    private static final AppConfig CONFIG = AppConfig.getInstance();

    @BeforeAll
    static void initialize() {
        CONFIG.initializeDatabase();
        assertTrue(CONFIG.isDatabaseReady(), "Laragon MySQL must be running for integration verification");
    }

    @Test
    void createsReadsUpdatesSearchesSoftDeletesAndRestoresEntry() throws SQLException {
        String marker = "verification-" + System.nanoTime();
        Entry created = null;
        try {
            Mood mood = CONFIG.moodService().getMoods().getFirst();
            created = CONFIG.entryService().save(null, "Life Lately " + marker,
                    "A temporary entry used to verify the complete journal data flow.", mood,
                    LocalDate.now(), true, marker);

            long createdId = created.getId();
            assertTrue(createdId > 0);
            assertEquals(marker, created.getTags().getFirst().name());
            assertEquals(createdId, CONFIG.entryService().getEntry(createdId).orElseThrow().getId());
            assertTrue(CONFIG.entryService().search(marker, null, null, false, "NEWEST").stream()
                    .anyMatch(entry -> entry.getId() == createdId));

            Entry updated = CONFIG.entryService().save(createdId, "Updated " + marker,
                    "The update path works too.", mood, LocalDate.now().minusDays(1), false, marker);
            assertEquals("Updated " + marker, updated.getTitle());
            assertFalse(updated.isFavorite());

            CONFIG.entryService().delete(createdId);
            assertTrue(CONFIG.entryService().getEntry(createdId).isEmpty());
            CONFIG.entryService().restore(createdId);
            assertTrue(CONFIG.entryService().getEntry(createdId).isPresent());
        } finally {
            if (created != null) removeVerificationData(created.getId(), marker);
        }
    }

    @Test
    void everyFxmlViewLoads() throws Exception {
        startJavaFx();
        List<String> resources = List.of(
                "/com/lifelately/fxml/main.fxml",
                "/com/lifelately/fxml/home/home.fxml",
                "/com/lifelately/fxml/journal/journal.fxml",
                "/com/lifelately/fxml/journal/entry-editor.fxml",
                "/com/lifelately/fxml/journal/entry-details.fxml",
                "/com/lifelately/fxml/calendar/calendar.fxml",
                "/com/lifelately/fxml/insights/insights.fxml",
                "/com/lifelately/fxml/settings/settings.fxml",
                "/com/lifelately/fxml/components/sidebar.fxml",
                "/com/lifelately/fxml/components/entry-card.fxml",
                "/com/lifelately/fxml/components/mood-selector.fxml",
                "/com/lifelately/fxml/components/empty-state.fxml"
        );
        for (String resource : resources) {
            runOnJavaFxThread(() -> assertNotNull(new FXMLLoader(App.class.getResource(resource)).load(), resource));
        }
    }

    @Test
    void lightAndDarkThemesApplyToTheLiveShell() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Parent root = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/main.fxml")).load();
            Scene scene = new Scene(root, 1240, 820);
            ThemeManager.initialize(scene);

            ThemeManager.apply(Theme.LIGHT, "BLUE");
            root.applyCss();
            root.layout();
            assertTrue(scene.getStylesheets().stream().anyMatch(path -> path.endsWith("light-theme.css")));
            assertTrue(root.getStyleClass().contains("accent-blue"));

            ThemeManager.apply(Theme.DARK, "CORAL");
            root.applyCss();
            root.layout();
            assertTrue(scene.getStylesheets().stream().anyMatch(path -> path.endsWith("dark-theme.css")));
            assertTrue(root.getStyleClass().contains("accent-coral"));
            assertFalse(root.getStyleClass().contains("accent-blue"));
        });
    }

    private static void startJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);
    }

    private static void runOnJavaFxThread(CheckedRunnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX action timed out");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private void removeVerificationData(long entryId, String tagName) throws SQLException {
        DatabaseConnection connectionProvider = new DatabaseConnection(DatabaseConfig.load());
        try (var connection = connectionProvider.getConnection()) {
            try (var deleteEntry = connection.prepareStatement("DELETE FROM entries WHERE id = ?")) {
                deleteEntry.setLong(1, entryId);
                deleteEntry.executeUpdate();
            }
            try (var deleteTag = connection.prepareStatement("DELETE FROM tags WHERE name = ?")) {
                deleteTag.setString(1, tagName);
                deleteTag.executeUpdate();
            }
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
