package com.lifelately;

import com.lifelately.config.AppConfig;
import com.lifelately.config.DatabaseConfig;
import com.lifelately.database.DatabaseConnection;
import com.lifelately.dao.UserDAO;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.model.User;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
    private static final DatabaseConnection TEST_DATABASE = new DatabaseConnection(DatabaseConfig.load());
    private static final UserDAO TEST_USERS = new UserDAO(TEST_DATABASE);
    private static final String TEST_USERNAME = "verify_user_" + System.nanoTime();
    private static final String TEST_PASSWORD = "VerificationPass123";
    private static Long testUserId;
    private static String previousRememberedUser;

    @BeforeAll
    static void initialize() {
        CONFIG.initializeDatabase();
        assertTrue(CONFIG.isDatabaseReady(), "Laragon MySQL must be running for integration verification");
        previousRememberedUser = readRememberedUser();
        User testUser = TEST_USERS.insert(TEST_USERNAME, "Verification User", "dGVzdA==", "dGVzdA==");
        testUserId = testUser.id();
        switchToTestUser(testUserId);
    }

    @AfterEach
    void keepTestAccountActive() {
        switchToTestUser(testUserId);
    }

    @AfterAll
    static void cleanupAccount() throws SQLException {
        CONFIG.authService().logout();
        try (var connection = TEST_DATABASE.getConnection();
             var statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            statement.setLong(1, testUserId);
            statement.executeUpdate();
        }
        restoreRememberedUser(previousRememberedUser);
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
                "/com/lifelately/fxml/auth/login.fxml",
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
    void expectedDatabaseTablesAreVisible() throws SQLException {
        DatabaseConnection connectionProvider = new DatabaseConnection(DatabaseConfig.load());
        List<String> expected = List.of("users", "moods", "entries", "tags", "entry_tags", "app_settings", "user_settings");
        try (var connection = connectionProvider.getConnection()) {
            for (String table : expected) {
                try (var result = connection.getMetaData().getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
                    assertTrue(result.next(), "Missing MySQL table: " + table);
                }
            }
        }
    }

    @Test
    void primaryButtonStylesSidebarActionsAndFixedGraphicsWork() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Parent login = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/auth/login.fxml")).load();
            Button submit = (Button) login.lookup("#submitButton");
            Button modeButton = (Button) login.lookup("#modeButton");
            assertNotNull(submit);
            assertNotNull(modeButton);
            assertTrue(submit.getStyleClass().contains("primary-button"));
            assertTrue(submit.getStyleClass().contains("login-submit"));
            modeButton.fire();
            assertEquals("Create account", submit.getText());

            Parent main = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/main.fxml")).load();
            Scene scene = new Scene(main, 1240, 820);
            ThemeManager.initialize(scene);
            ThemeManager.apply(Theme.LIGHT, "BLUE");
            main.applyCss();
            main.layout();

            Region moodIcon = (Region) main.lookup(".mood-icon");
            Region statSymbol = (Region) main.lookup(".stat-symbol");
            Region accentMark = (Region) main.lookup(".accent-mark");
            assertTrue(moodIcon.getMaxWidth() <= 24, "Mood icon may stretch across its button");
            assertTrue(statSymbol.getMaxWidth() <= 20, "Stat icon may stretch across its card");
            assertTrue(accentMark.getMaxWidth() <= 12, "Accent mark may stretch across its container");

            for (String id : List.of("homeButton", "journalButton", "calendarButton", "insightsButton", "settingsButton")) {
                Button button = (Button) main.lookup("#" + id);
                assertNotNull(button, id);
                button.fire();
                assertTrue(button.getStyleClass().contains("sidebar-item-active"), id + " action did not navigate");
                if (id.equals("calendarButton")) {
                    main.applyCss();
                    main.layout();
                    GridPane grid = (GridPane) main.lookup("#calendarGrid");
                    assertNotNull(grid);
                    assertEquals(7, grid.getColumnConstraints().size(), "Calendar must span seven responsive columns");

                    Button firstDay = grid.getChildren().stream()
                            .filter(Button.class::isInstance)
                            .map(Button.class::cast)
                            .filter(day -> day.getText().equals("1"))
                            .findFirst().orElseThrow();
                    firstDay.fire();
                    Button addMemory = (Button) main.lookup("#addDateEntryButton");
                    assertNotNull(addMemory);
                    addMemory.fire();
                    main.applyCss();
                    main.layout();
                    DatePicker editorDate = (DatePicker) main.lookup("#entryDatePicker");
                    assertNotNull(editorDate);
                    assertEquals(LocalDate.now().withDayOfMonth(1), editorDate.getValue(),
                            "Calendar add action must preselect the clicked date");
                }
            }
        });
    }

    @Test
    void accountsCannotReadOrChangeEachOthersEntries() throws SQLException {
        String firstMarker = "private-first-" + System.nanoTime();
        String secondMarker = "private-second-" + System.nanoTime();
        Entry firstEntry = null;
        Entry secondEntry = null;
        User secondUser = null;
        try {
            Mood mood = CONFIG.moodService().getMoods().getFirst();
            firstEntry = CONFIG.entryService().save(null, firstMarker, "Visible only to the first test account.",
                    mood, LocalDate.now(), false, "private-first");

            secondUser = CONFIG.authService().createAccount(
                    "verify_second_" + System.nanoTime(), "Second Verification User",
                    TEST_PASSWORD, TEST_PASSWORD);
            assertTrue(CONFIG.entryService().getEntry(firstEntry.getId()).isEmpty());
            assertTrue(CONFIG.entryService().search(firstMarker, null, null, false, "NEWEST").isEmpty());

            secondEntry = CONFIG.entryService().save(null, secondMarker, "Visible only to the second test account.",
                    mood, LocalDate.now(), false, "private-second");
            CONFIG.authService().login(secondUser.username(), TEST_PASSWORD);
            assertEquals(secondEntry.getId(), CONFIG.entryService().getEntry(secondEntry.getId()).orElseThrow().getId());

            switchToTestUser(testUserId);
            assertEquals(firstEntry.getId(), CONFIG.entryService().getEntry(firstEntry.getId()).orElseThrow().getId());
            assertTrue(CONFIG.entryService().getEntry(secondEntry.getId()).isEmpty());
        } finally {
            switchToTestUser(testUserId);
            try (var connection = TEST_DATABASE.getConnection()) {
                if (firstEntry != null) deleteEntry(connection, firstEntry.getId());
                if (secondUser != null) {
                    try (var deleteUser = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
                        deleteUser.setLong(1, secondUser.id());
                        deleteUser.executeUpdate();
                    }
                }
                deleteTag(connection, "private-first");
                deleteTag(connection, "private-second");
            }
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
        try (var connection = TEST_DATABASE.getConnection()) {
            deleteEntry(connection, entryId);
            deleteTag(connection, tagName);
        }
    }

    private static void switchToTestUser(long userId) {
        CONFIG.authService().logout();
        TEST_USERS.rememberUser(userId);
        assertTrue(CONFIG.authService().restoreSession(), "Could not activate test account");
    }

    private static String readRememberedUser() {
        try (var connection = TEST_DATABASE.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT setting_value FROM app_settings WHERE setting_key = 'remembered_user_id'");
             var result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : null;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not preserve the existing session.", exception);
        }
    }

    private static void restoreRememberedUser(String userId) throws SQLException {
        try (var connection = TEST_DATABASE.getConnection()) {
            if (userId == null) {
                try (var statement = connection.prepareStatement(
                        "DELETE FROM app_settings WHERE setting_key = 'remembered_user_id'")) {
                    statement.executeUpdate();
                }
            } else {
                try (var statement = connection.prepareStatement("""
                        INSERT INTO app_settings (setting_key, setting_value) VALUES ('remembered_user_id', ?)
                        ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
                        """)) {
                    statement.setString(1, userId);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static void deleteEntry(java.sql.Connection connection, long entryId) throws SQLException {
        try (var statement = connection.prepareStatement("DELETE FROM entries WHERE id = ?")) {
            statement.setLong(1, entryId);
            statement.executeUpdate();
        }
    }

    private static void deleteTag(java.sql.Connection connection, String tagName) throws SQLException {
        try (var statement = connection.prepareStatement("DELETE FROM tags WHERE name = ?")) {
            statement.setString(1, tagName);
            statement.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
