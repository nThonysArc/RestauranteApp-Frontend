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
import proyectopos.restauranteappfrontend.MainApplication; // <-- MODIFICADO: Necesario para cargar CSS
import proyectopos.restauranteappfrontend.model.LoginResponse; // <-- MODIFICADO: Necesario para cargar CSS
import proyectopos.restauranteappfrontend.services.AuthService;
import proyectopos.restauranteappfrontend.util.SessionManager; // <-- AÑADIR IMPORT

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        messageLabel.getStyleClass().setAll("lbl-info"); // Estilo por defecto Bootstrap: informativo
    }
    @FXML
    protected void onLoginButtonClick() {
        final String username = usernameField.getText();
        final String password = passwordField.getText();

        // Validaciones básicas en el frontend (opcional pero recomendado)
        if (username.isBlank() || password.isBlank()) {
            messageLabel.setText("Usuario y contraseña son requeridos.");
            messageLabel.getStyleClass().setAll("lbl-danger");
            return;
        }

        setControlsDisabled(true);
        messageLabel.setText("Verificando credenciales...");
        messageLabel.getStyleClass().setAll("lbl-warning");

        // --- INICIO: Llamada Real al Backend en Hilo Separado ---
        // Usamos un nuevo hilo para no bloquear la UI durante la llamada de red
        new Thread(() -> {
            try {
               // <-- MODIFICADO: Esperamos un objeto LoginResponse, no un String
                LoginResponse response = authService.authenticate(username, password);

                Platform.runLater(() -> {
                    // <-- MODIFICADO: Verificamos el objeto
                    if (response != null) { 
                        messageLabel.setText("¡Inicio de sesión exitoso!");
                        messageLabel.getStyleClass().setAll("lbl-success");

                        // <-- MODIFICADO: Guardamos el objeto completo en la sesión
                        SessionManager.getInstance().setLoginResponse(response);
                        System.out.println("Login exitoso. Rol: " + response.getRol());

                        try {
                            navigateToMainView(); 
                        } catch (IOException e) {
                            handleUIError("Error interno al cargar la app.", e);
                        }

                    } else {
                        // El AuthService devolvió null (falló la autenticación)
                        messageLabel.setText("Credenciales incorrectas. Intente de nuevo.");
                        messageLabel.getStyleClass().setAll("lbl-danger");
                        setControlsDisabled(false);
                        passwordField.clear();
                    }
                });

            } catch (IOException | InterruptedException e) {
                // Error de red o interrupción
                Platform.runLater(() -> {
                    handleUIError("Error de conexión con el servidor.", e);
                });
            } catch (Exception e) {
                // Cualquier otro error inesperado
                Platform.runLater(() -> {
                    handleUIError("Error inesperado durante el login.", e);
                });
            }
        }).start(); // Inicia el hilo
        // --- FIN: Llamada Real al Backend ---
    }

    /**
     * Carga y muestra la ventana principal de la aplicación (MainView),
     * que actúa como el "caparazón" o enrutador.
     * @throws IOException si el archivo FXML no se encuentra.
     */
    // <-- MODIFICADO: Método 'navigateToDashboard' renombrado y actualizado
    private void navigateToMainView() throws IOException {
        // Obtiene el Stage (ventana) actual a partir del botón de login
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        
        // 1. Carga el NUEVO "main-view.fxml"
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/main-view.fxml"));
        Parent root = loader.load();

        // 2. Crea una nueva escena (más grande para la app principal)
        Scene scene = new Scene(root, 1280, 720); // Tamaño más grande
        
        // 3. Aplica los estilos (BootstrapFX y nuestro tema oscuro)
        scene.getStylesheets().add(org.kordamp.bootstrapfx.BootstrapFX.bootstrapFXStylesheet());
        
        URL cssUrl = MainApplication.class.getResource("/proyectopos/restauranteappfrontend/dark-theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Error: No se pudo cargar dark-theme.css en LoginController");
        }

        currentStage.setTitle("Restaurante POS"); // Título general de la app
        currentStage.setScene(scene);
        currentStage.setResizable(true); // Permitir redimensionar la app principal
        currentStage.centerOnScreen();
        currentStage.show();
    }
    // <-- FIN MODIFICADO

    /**
     * Habilita o deshabilita los controles durante el proceso de login.
     * @param disabled true para deshabilitar, false para habilitar.
     */
    private void setControlsDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }
    
    // Método auxiliar para manejar errores y mostrar en la interfaz
    private void handleUIError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace(); // Imprime el stack trace completo en la consola
        messageLabel.setText(message);
        messageLabel.getStyleClass().setAll("lbl-danger");
        setControlsDisabled(false); // Habilitar controles
    }
}