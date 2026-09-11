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
    public void start(Stage stage) throws IOException {
        AppConfig config = AppConfig.getInstance();
        config.initializeDatabase();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/lifelately/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1240, 820);
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

    public static void main(String[] args) {
        launch(args);
    }
}
