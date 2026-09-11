package com.lifelately.navigation;

public enum View {
    HOME("/com/lifelately/fxml/home/home.fxml"),
    JOURNAL("/com/lifelately/fxml/journal/journal.fxml"),
    ENTRY_EDITOR("/com/lifelately/fxml/journal/entry-editor.fxml"),
    ENTRY_DETAILS("/com/lifelately/fxml/journal/entry-details.fxml"),
    CALENDAR("/com/lifelately/fxml/calendar/calendar.fxml"),
    INSIGHTS("/com/lifelately/fxml/insights/insights.fxml"),
    SETTINGS("/com/lifelately/fxml/settings/settings.fxml");

    private final String fxml;

    View(String fxml) { this.fxml = fxml; }
    public String fxml() { return fxml; }
}
