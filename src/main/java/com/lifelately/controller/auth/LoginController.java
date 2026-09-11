package com.lifelately.controller.auth;

import com.lifelately.App;
import com.lifelately.config.AppConfig;
import com.lifelately.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class LoginController {
    @FXML private StackPane loginRoot;
    @FXML private Label formEyebrow;
    @FXML private Label formTitle;
    @FXML private Label formSubtitle;
    @FXML private VBox displayNameGroup;
    @FXML private VBox confirmationGroup;
    @FXML private TextField displayNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmationField;
    @FXML private Button submitButton;
    @FXML private Button modeButton;
    @FXML private Label modePrompt;
    @FXML private Label statusLabel;

    private final AppConfig config = AppConfig.getInstance();
    private boolean registrationMode;

    @FXML
    private void initialize() {
        if (!config.isDatabaseReady()) {
            formEyebrow.setText("DATABASE CONNECTION");
            formTitle.setText("MySQL is not available");
            formSubtitle.setText("Start MySQL in Laragon, then restart Life Lately.");
            submitButton.setDisable(true);
            showError(config.databaseMessage());
            return;
        }

        setMode(config.authService().requiresFirstAccount());
    }

    @FXML
    private void submit() {
        clearStatus();
        try {
            AuthService auth = config.authService();
            if (registrationMode) {
                auth.createAccount(usernameField.getText(), displayNameField.getText(),
                        passwordField.getText(), confirmationField.getText());
            } else {
                auth.login(usernameField.getText(), passwordField.getText());
            }
            App.showMain(loginRoot.getScene());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void toggleMode() {
        setMode(!registrationMode);
        clearStatus();
        passwordField.clear();
        confirmationField.clear();
    }

    private void setMode(boolean register) {
        registrationMode = register;
        displayNameGroup.setVisible(register);
        displayNameGroup.setManaged(register);
        confirmationGroup.setVisible(register);
        confirmationGroup.setManaged(register);
        if (register) {
            formEyebrow.setText("CREATE ACCOUNT");
            formTitle.setText("Start your own journal");
            formSubtitle.setText("Each account has separate entries and personal settings.");
            submitButton.setText("Create account");
            modePrompt.setText("Already have an account?");
            modeButton.setText("Sign in");
        } else {
            formEyebrow.setText("WELCOME BACK");
            formTitle.setText("Sign in to your journal");
            formSubtitle.setText("Your entries and settings stay private to your account.");
            submitButton.setText("Sign in");
            modePrompt.setText("New to Life Lately?");
            modeButton.setText("Create an account");
        }
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showError(String message) {
        statusLabel.setText(message == null || message.isBlank() ? "Unable to continue." : message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
