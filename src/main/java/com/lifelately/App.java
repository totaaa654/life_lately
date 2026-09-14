package com.lifelately;

import com.lifelately.config.AppConfig;
import com.lifelately.model.AppSettings;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public final class App extends Application {
    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.getInstance();
        config.initializeDatabase();

        boolean restoreJournal = config.isDatabaseReady() && config.authService().restoreSession();
        Scene scene = new Scene(loadRoot(restoreJournal
                ? "/com/lifelately/fxml/main.fxml"
                : "/com/lifelately/fxml/auth/login.fxml"));
        ThemeManager.initialize(scene);
        AppSettings settings = restoreJournal
                ? config.settingsService().getSettings()
                : AppSettings.defaults();
        ThemeManager.apply(Theme.valueOf(settings.theme()), settings.accentColor());

        stage.setTitle("Life Lately");
        stage.getIcons().add(new Image(App.class.getResourceAsStream(
                "/com/lifelately/images/brand/app-icon.png")));
        stage.setScene(scene);
        sizeToScreen(stage, Screen.getPrimary().getVisualBounds());
        stage.show();
    }

    public static void showMain(Scene scene) {
        scene.setRoot(loadRoot("/com/lifelately/fxml/main.fxml"));
        AppSettings settings = AppConfig.getInstance().settingsService().getSettings();
        ThemeManager.apply(Theme.valueOf(settings.theme()), settings.accentColor());
    }

    public static void showLogin(Scene scene) {
        scene.setRoot(loadRoot("/com/lifelately/fxml/auth/login.fxml"));
        ThemeManager.refreshRoot();
    }

    static void sizeToScreen(Stage stage, Rectangle2D visualBounds) {
        double width = Math.min(1440, visualBounds.getWidth() * 0.92);
        double height = Math.min(900, visualBounds.getHeight() * 0.90);
        stage.setMinWidth(Math.min(1050, visualBounds.getWidth()));
        stage.setMinHeight(Math.min(640, visualBounds.getHeight()));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - width) / 2);
        stage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - height) / 2);
    }

    private static javafx.scene.Parent loadRoot(String resource) {
        try {
            return new FXMLLoader(App.class.getResource(resource)).load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not open application view: " + resource, exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
