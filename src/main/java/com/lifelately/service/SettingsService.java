package com.lifelately.service;

import com.lifelately.dao.SettingsDAO;
import com.lifelately.model.AppSettings;

import java.util.Map;

public final class SettingsService {
    private final SettingsDAO settingsDAO;

    public SettingsService(SettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
    }

    public AppSettings getSettings() {
        Map<String, String> values = settingsDAO.findAll();
        AppSettings defaults = AppSettings.defaults();
        return new AppSettings(
                values.getOrDefault("theme", defaults.theme()),
                values.getOrDefault("accent_color", defaults.accentColor()),
                values.getOrDefault("date_format", defaults.dateFormat()),
                values.getOrDefault("first_day_of_week", defaults.firstDayOfWeek())
        );
    }

    public void save(AppSettings settings) {
        settingsDAO.save("theme", settings.theme());
        settingsDAO.save("accent_color", settings.accentColor());
        settingsDAO.save("date_format", settings.dateFormat());
        settingsDAO.save("first_day_of_week", settings.firstDayOfWeek());
    }
}
