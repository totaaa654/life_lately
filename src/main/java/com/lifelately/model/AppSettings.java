package com.lifelately.model;

public record AppSettings(String theme, String accentColor, String dateFormat, String firstDayOfWeek) {
    public static AppSettings defaults() {
        return new AppSettings("SYSTEM", "BLUE", "MMM d, yyyy", "MONDAY");
    }
}
