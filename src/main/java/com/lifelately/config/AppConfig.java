package com.lifelately.config;

import com.lifelately.dao.EntryDAO;
import com.lifelately.dao.MoodDAO;
import com.lifelately.dao.SettingsDAO;
import com.lifelately.dao.TagDAO;
import com.lifelately.database.DatabaseConnection;
import com.lifelately.database.DatabaseInitializer;
import com.lifelately.service.EntryService;
import com.lifelately.service.MoodService;
import com.lifelately.service.SettingsService;
import com.lifelately.service.TagService;

public final class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    private final DatabaseConnection databaseConnection;
    private final DatabaseInitializer databaseInitializer;
    private final EntryService entryService;
    private final MoodService moodService;
    private final TagService tagService;
    private final SettingsService settingsService;
    private boolean databaseReady;
    private String databaseMessage = "Database has not been initialized.";

    private AppConfig() {
        databaseConnection = new DatabaseConnection(DatabaseConfig.load());
        databaseInitializer = new DatabaseInitializer(databaseConnection);
        TagDAO tagDAO = new TagDAO(databaseConnection);
        tagService = new TagService(tagDAO);
        entryService = new EntryService(new EntryDAO(databaseConnection), tagService);
        moodService = new MoodService(new MoodDAO(databaseConnection));
        settingsService = new SettingsService(new SettingsDAO(databaseConnection));
    }

    public static AppConfig getInstance() { return INSTANCE; }

    public void initializeDatabase() {
        try {
            databaseInitializer.initialize();
            databaseReady = true;
            databaseMessage = "Connected to life_lately";
        } catch (Exception exception) {
            databaseReady = false;
            databaseMessage = exception.getMessage();
            System.err.println("Life Lately database: " + exception.getMessage());
        }
    }

    public EntryService entryService() { return entryService; }
    public MoodService moodService() { return moodService; }
    public TagService tagService() { return tagService; }
    public SettingsService settingsService() { return settingsService; }
    public boolean isDatabaseReady() { return databaseReady; }
    public String databaseMessage() { return databaseMessage; }
}
