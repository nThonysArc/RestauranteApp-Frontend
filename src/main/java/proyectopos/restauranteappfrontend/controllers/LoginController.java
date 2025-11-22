package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences; // Importar Preferences

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox; // Importar CheckBox
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
    
    // Nuevos componentes inyectados desde el FXML actualizado
    @FXML private TextField passwordTextField; // Campo visible
    @FXML private Button togglePasswordBtn;    // Botón del ojo
    @FXML private CheckBox rememberMeCheckBox; // Checkbox recordar

    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    
    private final AuthService authService = new AuthService();
    private static final String RAIZ_IQUENA_THEME_CSS = "/proyectopos/restauranteappfrontend/raiz-iquena-theme.css";

    // Preferencias para guardar datos (se guardan en el registro del SO o carpeta de usuario)
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password"; // Nota: En producción real, esto debería encriptarse.
    private static final String PREF_REMEMBER = "remember";

    @FXML
    public void initialize() {
        messageLabel.getStyleClass().setAll("lbl-info");

        // 1. Sincronizar el texto entre el campo oculto y el visible
        // Esto asegura que si escribes en uno, el otro también se actualice.
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        // 2. Cargar credenciales guardadas si existen
        cargarPreferencias();
    }

    // --- Lógica del Ojo (Ver Contraseña) ---
    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            // Cambiar a modo visible
            passwordField.setVisible(false);
            passwordField.setManaged(false); // Quitar del layout para que el otro ocupe su lugar
            
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            
            togglePasswordBtn.setText("🙈"); // Icono tachado (ocultar)
        } else {
            // Cambiar a modo oculto (asteriscos)
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            
            togglePasswordBtn.setText("👁"); // Icono normal (ver)
        }
    }

    // --- Gestión de Preferencias (Recordar Usuario) ---
    private void cargarPreferencias() {
        boolean remember = prefs.getBoolean(PREF_REMEMBER, false);
        if (remember) {
            String savedUser = prefs.get(PREF_USERNAME, "");
            String savedPass = prefs.get(PREF_PASSWORD, "");
            
            usernameField.setText(savedUser);
            passwordField.setText(savedPass); // Al setear este, el bind actualiza passwordTextField también
            rememberMeCheckBox.setSelected(true);
        }
    }

    private void guardarPreferencias() {
        if (rememberMeCheckBox.isSelected()) {
            prefs.put(PREF_USERNAME, usernameField.getText());
            prefs.put(PREF_PASSWORD, passwordField.getText());
            prefs.putBoolean(PREF_REMEMBER, true);
        } else {
            // Si el usuario desmarca, borramos los datos guardados por seguridad
            prefs.remove(PREF_USERNAME);
            prefs.remove(PREF_PASSWORD);
            prefs.putBoolean(PREF_REMEMBER, false);
        }
    }

    @FXML
    protected void onLoginButtonClick() {
        final String username = usernameField.getText();
        // Siempre obtenemos la contraseña del campo PasswordField (es la fuente de verdad)
        final String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
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
                        // ¡Login Exitoso! -> Guardamos preferencias
                        guardarPreferencias();

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
                        // No limpiamos username para facilitar reintento
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
        if (passwordTextField != null) passwordTextField.setDisable(disabled);
        if (rememberMeCheckBox != null) rememberMeCheckBox.setDisable(disabled);
        if (togglePasswordBtn != null) togglePasswordBtn.setDisable(disabled);
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