package com.lifelately;

import com.lifelately.config.AppConfig;
import com.lifelately.model.AppSettings;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class App extends Application {
    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.getInstance();
        config.initializeDatabase();

        Scene scene = new Scene(loadRoot("/com/lifelately/fxml/auth/login.fxml"), 1240, 820);
        ThemeManager.initialize(scene);
        AppSettings settings = config.isDatabaseReady()
                ? config.settingsService().getSettings()
                : AppSettings.defaults();
        ThemeManager.apply(Theme.valueOf(settings.theme()), settings.accentColor());

        stage.setTitle("Life Lately");
        stage.setMinWidth(960);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void showMain(Scene scene) {
        scene.setRoot(loadRoot("/com/lifelately/fxml/main.fxml"));
        ThemeManager.refreshRoot();
    }

    public static void showLogin(Scene scene) {
        scene.setRoot(loadRoot("/com/lifelately/fxml/auth/login.fxml"));
        ThemeManager.refreshRoot();
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
