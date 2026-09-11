package com.lifelately.theme;

import javafx.scene.Scene;

import java.util.List;

public final class ThemeManager {
    private static final String RESOURCE_ROOT = "/com/lifelately/css/";
    private static final List<String> ACCENT_CLASSES = List.of(
            "accent-blue", "accent-yellow", "accent-green", "accent-lavender", "accent-coral");
    private static Scene scene;

    private ThemeManager() { }

    public static void initialize(Scene applicationScene) {
        scene = applicationScene;
        addStylesheet("base.css");
        addStylesheet("components/sidebar.css");
        addStylesheet("components/buttons.css");
        addStylesheet("components/cards.css");
        addStylesheet("components/forms.css");
        addStylesheet("components/mood-selector.css");
        addStylesheet("pages/home.css");
        addStylesheet("pages/journal.css");
        addStylesheet("pages/calendar.css");
        addStylesheet("pages/insights.css");
        addStylesheet("pages/settings.css");
    }

    public static void apply(Theme requestedTheme, String accent) {
        if (scene == null) return;
        scene.getStylesheets().removeIf(sheet -> sheet.endsWith("light-theme.css") || sheet.endsWith("dark-theme.css"));
        Theme effective = requestedTheme == Theme.SYSTEM ? Theme.LIGHT : requestedTheme;
        addStylesheet(effective == Theme.DARK ? "dark-theme.css" : "light-theme.css");

        scene.getRoot().getStyleClass().removeAll(ACCENT_CLASSES);
        scene.getRoot().getStyleClass().add("accent-" + accent.toLowerCase());
    }

    private static void addStylesheet(String path) {
        String resource = ThemeManager.class.getResource(RESOURCE_ROOT + path).toExternalForm();
        if (!scene.getStylesheets().contains(resource)) scene.getStylesheets().add(resource);
    }
}
