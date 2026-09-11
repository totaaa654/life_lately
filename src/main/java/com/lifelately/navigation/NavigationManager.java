package com.lifelately.navigation;

import com.lifelately.controller.journal.EntryDetailsController;
import com.lifelately.controller.journal.EntryEditorController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.function.Consumer;

public final class NavigationManager {
    private static NavigationManager instance;
    private final StackPane contentHost;
    private final Consumer<View> navigationListener;

    public NavigationManager(StackPane contentHost, Consumer<View> navigationListener) {
        this.contentHost = contentHost;
        this.navigationListener = navigationListener;
        instance = this;
    }

    public static NavigationManager getInstance() {
        if (instance == null) throw new IllegalStateException("Navigation is not ready yet.");
        return instance;
    }

    public void navigate(View view) {
        load(view, controller -> { });
    }

    public void openEditor(Long entryId) {
        openEditor(entryId, null);
    }

    public void openEditor(Long entryId, String moodName) {
        load(View.ENTRY_EDITOR, controller -> {
            EntryEditorController editor = (EntryEditorController) controller;
            editor.setEntry(entryId);
            if (entryId == null && moodName != null) editor.selectMood(moodName);
        });
    }

    public void openDetails(long entryId) {
        load(View.ENTRY_DETAILS, controller -> ((EntryDetailsController) controller).setEntry(entryId));
    }

    private void load(View view, Consumer<Object> configureController) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(view.fxml()));
            Parent content = loader.load();
            configureController.accept(loader.getController());
            contentHost.getChildren().setAll(content);
            navigationListener.accept(view);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not open " + view.name().toLowerCase() + " view.", exception);
        }
    }
}
