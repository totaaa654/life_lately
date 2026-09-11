package com.lifelately.service;

import com.lifelately.dao.SettingsDAO;
import com.lifelately.model.AppSettings;

import java.util.Map;

public final class SettingsService {
    private final SettingsDAO settingsDAO;
    private final AuthService authService;

    public SettingsService(SettingsDAO settingsDAO, AuthService authService) {
        this.settingsDAO = settingsDAO;
        this.authService = authService;
    }

    public AppSettings getSettings() {
        Map<String, String> values = settingsDAO.findAll(currentUserId());
        AppSettings defaults = AppSettings.defaults();
        return new AppSettings(
                values.getOrDefault("theme", defaults.theme()),
                values.getOrDefault("accent_color", defaults.accentColor()),
                values.getOrDefault("date_format", defaults.dateFormat()),
                values.getOrDefault("first_day_of_week", defaults.firstDayOfWeek())
        );
    }

    public void save(AppSettings settings) {
        long userId = currentUserId();
        settingsDAO.save(userId, "theme", settings.theme());
        settingsDAO.save(userId, "accent_color", settings.accentColor());
        settingsDAO.save(userId, "date_format", settings.dateFormat());
        settingsDAO.save(userId, "first_day_of_week", settings.firstDayOfWeek());
    }

    private long currentUserId() {
        return authService.currentUser()
                .orElseThrow(() -> new IllegalStateException("Sign in to access journal settings."))
                .id();
    }
}
