package proyectopos.restauranteappfrontend.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import proyectopos.restauranteappfrontend.services.AuthService;
import proyectopos.restauranteappfrontend.util.SessionManager;

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
                // Llamamos al servicio de autenticación
                String receivedToken = authService.authenticate(username, password);

                // Volvemos al hilo de JavaFX para actualizar la UI
                Platform.runLater(() -> {
                    if (receivedToken != null) {
                        messageLabel.setText("¡Inicio de sesión exitoso!");
                        messageLabel.getStyleClass().setAll("lbl-success");

                        // ❗️ PASO IMPORTANTE: Guardar el token para futuras peticiones
                        // Por ahora, lo guardaremos en una variable estática simple
                        // (En una app real, usarías un gestor de sesión/estado)
                        SessionManager.getInstance().setToken(receivedToken);
                        System.out.println("Token recibido y guardado: " + receivedToken); // Para depuración

                        try {
                            navigateToDashboard();
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
     * Carga y muestra la ventana principal de la aplicación (Dashboard), reemplazando la de login.
     * @throws IOException si el archivo FXML no se encuentra.
     */
    private void navigateToDashboard() throws IOException {
        // Obtiene el Stage (ventana) actual a partir del botón de login
        Stage currentStage = (Stage) loginButton.getScene().getWindow();

        // Carga el nuevo FXML del Dashboard
        // Nota: La ruta debe coincidir con la ubicación en la carpeta resources
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/dashboard-view.fxml"));
        Parent root = loader.load();

        // Crea una nueva escena
        Scene scene = new Scene(root);

        // Aplica el estilo BootstrapFX a la nueva escena
        scene.getStylesheets().add(org.kordamp.bootstrapfx.BootstrapFX.bootstrapFXStylesheet());

        // Configura y muestra el nuevo Stage
        currentStage.setTitle("Restaurante POS - Panel Principasl");
        currentStage.setScene(scene);
        currentStage.setResizable(true); // El Dashboard suele ser redimensionable
        currentStage.centerOnScreen();
        currentStage.show();
    }


    /**
     * Habilita o deshabilita los controles durante el proceso de login.
     * @param disabled true para deshabilitar, false para habilitar.
     */
    private void setControlsDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }
    // Método auxiliar para manejar errores y mostrar en UI
    private void handleUIError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace(); // Imprime el stack trace completo en la consola
        messageLabel.setText(message);
        messageLabel.getStyleClass().setAll("lbl-danger");
        setControlsDisabled(false); // Habilitar controles
    }
}
