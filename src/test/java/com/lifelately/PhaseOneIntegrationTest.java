package com.lifelately;

import com.lifelately.config.AppConfig;
import com.lifelately.config.DatabaseConfig;
import com.lifelately.controller.journal.EntryCardController;
import com.lifelately.database.DatabaseConnection;
import com.lifelately.dao.UserDAO;
import com.lifelately.model.AppSettings;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.model.User;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseOneIntegrationTest {
    private static final AppConfig CONFIG = AppConfig.getInstance();
    private static final DatabaseConnection TEST_DATABASE = new DatabaseConnection(DatabaseConfig.load());
    private static final UserDAO TEST_USERS = new UserDAO(TEST_DATABASE);
    private static final String TEST_USERNAME = "verify_user_" + System.nanoTime();
    private static final String TEST_PASSWORD = "VerificationPass123!";
    private static final String CHANGED_PASSWORD = "ChangedPassword456!";
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

    @Test
    void settingsAppearanceControlsReflectTheActivePreview() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Parent main = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/main.fxml")).load();
            Scene scene = new Scene(main, 1240, 820);
            ThemeManager.initialize(scene);
            ThemeManager.apply(Theme.DARK, "LAVENDER");

            Parent settings = new FXMLLoader(
                    App.class.getResource("/com/lifelately/fxml/settings/settings.fxml")).load();
            new Scene(settings);
            settings.applyCss();
            ComboBox<?> themeBox = (ComboBox<?>) settings.lookup("#themeBox");
            ComboBox<?> accentBox = (ComboBox<?>) settings.lookup("#accentBox");

            assertEquals("Dark", themeBox.getValue());
            assertEquals("Lavender", accentBox.getValue());
        });
    }

    @Test
    void registrationAllowsFriendlyUsernamesButRequiresUniqueNamesAndStrongPasswords() {
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class,
                () -> CONFIG.authService().createAccount(TEST_USERNAME.toUpperCase(), "Duplicate User",
                        TEST_PASSWORD, TEST_PASSWORD));
        assertEquals("That username is already taken.", duplicate.getMessage());

        IllegalArgumentException weakPassword = assertThrows(IllegalArgumentException.class,
                () -> CONFIG.authService().createAccount("Friendly name!", "Friendly User",
                        "alllowercase1!", "alllowercase1!"));
        assertEquals("Password needs at least one uppercase letter.", weakPassword.getMessage());
    }

    @Test
    void selectedCurrentDayKeepsItsMoodIndicatorVisible() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Button currentDay = new Button("14");
            currentDay.getStyleClass().addAll("calendar-day", "calendar-today", "calendar-selected",
                    "calendar-has-entry", "calendar-mood-low");
            StackPane root = new StackPane(currentDay);
            Scene scene = new Scene(root, 200, 120);
            ThemeManager.initialize(scene);
            ThemeManager.apply(Theme.LIGHT, "YELLOW");
            root.applyCss();

            var border = currentDay.getBorder().getStrokes().getFirst();
            assertEquals(Color.web("#e9b937"), border.getTopStroke());
            assertEquals(Color.web("#9a86da"), border.getBottomStroke(),
                    "The selected current day must retain its Low mood color");
        });
    }

    @Test
    void journalCardsOnlyShowTheFavoriteBadgeForFavorites() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Entry entry = new Entry();
            entry.setId(1);
            entry.setEntryDate(LocalDate.now());
            entry.setTitle("A regular memory");
            entry.setContent("Already saved, but not marked as a favorite.");
            entry.setMood(new Mood(1, "Good", "leaf", "#65AE90", 2));

            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("/com/lifelately/fxml/components/entry-card.fxml"));
            Parent card = loader.load();
            EntryCardController controller = loader.getController();
            controller.setEntry(entry);
            new Scene(card);
            card.applyCss();
            Label favoriteLabel = (Label) card.lookup("#favoriteLabel");

            assertFalse(favoriteLabel.isVisible());
            assertFalse(favoriteLabel.isManaged());

            entry.setFavorite(true);
            controller.setEntry(entry);
            card.applyCss();
            card.layout();
            Label moodLabel = (Label) card.lookup("#moodLabel");
            assertTrue(favoriteLabel.isVisible());
            assertTrue(favoriteLabel.isManaged());
            assertEquals("Favorite", favoriteLabel.getText());
            assertEquals(moodLabel.getBoundsInParent().getMinY(),
                    favoriteLabel.getBoundsInParent().getMinY(), 1,
                    "Mood and favorite badges should be vertically aligned");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void appearanceChangesArePersistedWithoutPressingThePreferencesButton() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Parent main = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/main.fxml")).load();
            Scene scene = new Scene(main, 1240, 820);
            ThemeManager.initialize(scene);
            AppSettings original = CONFIG.settingsService().getSettings();
            ThemeManager.apply(Theme.valueOf(original.theme()), original.accentColor());

            Parent settings = new FXMLLoader(
                    App.class.getResource("/com/lifelately/fxml/settings/settings.fxml")).load();
            new Scene(settings);
            settings.applyCss();
            ComboBox<String> themeBox = (ComboBox<String>) settings.lookup("#themeBox");
            ComboBox<String> accentBox = (ComboBox<String>) settings.lookup("#accentBox");

            themeBox.setValue("Dark");
            accentBox.setValue("Coral");

            AppSettings persisted = CONFIG.settingsService().getSettings();
            assertEquals("DARK", persisted.theme());
            assertEquals("CORAL", persisted.accentColor());
        });
    }

    @Test
    void signedInUserCanUpdateDisplayNameAndPassword() throws SQLException {
        String username = "account_settings_" + System.nanoTime();
        User account = null;
        try {
            account = CONFIG.authService().createAccount(username, "Original Name",
                    TEST_PASSWORD, TEST_PASSWORD);

            User updated = CONFIG.authService().updateDisplayName("Updated Name");
            assertEquals("Updated Name", updated.displayName());
            assertEquals("Updated Name", TEST_USERS.findById(account.id()).orElseThrow().displayName());

            CONFIG.authService().changePassword(TEST_PASSWORD, CHANGED_PASSWORD, CHANGED_PASSWORD);
            CONFIG.authService().logout();
            assertThrows(IllegalArgumentException.class,
                    () -> CONFIG.authService().login(username, TEST_PASSWORD));
            assertEquals(account.id(), CONFIG.authService().login(username, CHANGED_PASSWORD).id());
        } finally {
            CONFIG.authService().logout();
            if (account != null) {
                try (var connection = TEST_DATABASE.getConnection();
                     var statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    statement.setLong(1, account.id());
                    statement.executeUpdate();
                }
            }
            switchToTestUser(testUserId);
        }
    }

    @Test
    void startupWindowFitsAndCentersWithinTheUsableScreen() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Stage stage = new Stage();
            Rectangle2D scaledLaptopScreen = new Rectangle2D(0, 0, 1536, 824);
            App.sizeToScreen(stage, scaledLaptopScreen);

            assertEquals(1413.12, stage.getWidth(), 0.01);
            assertEquals(741.60, stage.getHeight(), 0.01);
            assertTrue(stage.getX() >= scaledLaptopScreen.getMinX());
            assertTrue(stage.getY() >= scaledLaptopScreen.getMinY());
            assertTrue(stage.getX() + stage.getWidth() <= scaledLaptopScreen.getMaxX());
            assertTrue(stage.getY() + stage.getHeight() <= scaledLaptopScreen.getMaxY());
            stage.close();
        });
    }

    @Test
    void homeMoodChoicesShareTheAvailableWidthWithoutSquashing() throws Exception {
        startJavaFx();
        runOnJavaFxThread(() -> {
            Parent home = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/home/home.fxml")).load();
            Scene scene = new Scene(home, 820, 720);
            ThemeManager.initialize(scene);
            ThemeManager.apply(Theme.LIGHT, "BLUE");
            home.applyCss();
            home.layout();

            List<ToggleButton> moodChoices = new ArrayList<>();
            home.lookupAll(".home-mood-choice").forEach(node -> moodChoices.add((ToggleButton) node));
            assertEquals(5, moodChoices.size());
            double firstWidth = moodChoices.getFirst().getWidth();
            assertTrue(firstWidth >= 120, "Home mood choices should remain comfortably wide");
            assertTrue(moodChoices.stream().allMatch(choice -> Math.abs(choice.getWidth() - firstWidth) <= 2),
                    "Mood choices should have equal widths apart from pixel rounding");
            assertTrue(moodChoices.stream().allMatch(choice -> choice.getHeight() >= 76));
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
