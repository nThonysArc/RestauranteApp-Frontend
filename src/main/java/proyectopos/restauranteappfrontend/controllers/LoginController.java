package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.net.URL;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import proyectopos.restauranteappfrontend.MainApplication;
import proyectopos.restauranteappfrontend.model.LoginResponse;
import proyectopos.restauranteappfrontend.services.AuthService;
import proyectopos.restauranteappfrontend.util.SessionManager;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    private final AuthService authService = new AuthService();

    private static final String RAIZ_IQUENA_THEME_CSS = "/proyectopos/restauranteappfrontend/raiz-iquena-theme.css";


    @FXML
    public void initialize() {
        messageLabel.getStyleClass().setAll("lbl-info");
    }

    @FXML
    protected void onLoginButtonClick() {
        final String username = usernameField.getText();
        final String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            messageLabel.setText("Usuario y contraseña son requeridos.");
            messageLabel.getStyleClass().setAll("lbl-danger");
            return;
        }

        setControlsDisabled(true);
        messageLabel.setText("Verificando credenciales...");
        messageLabel.getStyleClass().setAll("lbl-warning");
        ThreadManager.getInstance().execute(() -> {
            try {
                LoginResponse response = authService.authenticate(username, password);
                Platform.runLater(() -> {
                    if (response != null) {
                        messageLabel.setText("¡Inicio de sesión exitoso!");
                        messageLabel.getStyleClass().setAll("lbl-success");
                        SessionManager.getInstance().setLoginResponse(response);
                        System.out.println("Login exitoso. Rol: " + response.getRol());
                        try {
                            navigateToMainView();
                        } catch (IOException e) {
                            handleUIError("Error interno al cargar la app.", e);
                        }
                    } else {
                        messageLabel.setText("Credenciales incorrectas. Intente de nuevo.");
                        messageLabel.getStyleClass().setAll("lbl-danger");
                        setControlsDisabled(false);
                        passwordField.clear();
                    }
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> handleUIError("Error de conexión con el servidor.", e));
            } catch (Exception e) {
                Platform.runLater(() -> handleUIError("Error inesperado durante el login.", e));
            }
        }); 
    }

    private void navigateToMainView() throws IOException {
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/main-view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);

        scene.getStylesheets().add(org.kordamp.bootstrapfx.BootstrapFX.bootstrapFXStylesheet());

        // Cargar el tema Raíz Iqueña
        URL cssUrl = MainApplication.class.getResource(RAIZ_IQUENA_THEME_CSS); 
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Error: No se pudo cargar raiz-iquena-theme.css en LoginController");
        }

        currentStage.setTitle("Restaurante POS Raíz Iqueña"); 
        currentStage.setScene(scene);
        currentStage.setResizable(true);
        currentStage.centerOnScreen();
        currentStage.show();
    }

    private void setControlsDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }

    private void handleUIError(String message, Exception e) {
        System.err.println(message + ": " + (e != null ? e.getMessage() : ""));
        if(e != null) e.printStackTrace();
        messageLabel.setText(message);
        messageLabel.getStyleClass().setAll("lbl-danger");
        setControlsDisabled(false);
    }
}