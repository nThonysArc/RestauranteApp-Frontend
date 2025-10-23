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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginController {

    // Elementos FXML vinculados desde login-view.fxml
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Label messageLabel;

    // Servicio para simular latencia de red sin congelar la UI
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Se llama al inicializar el controlador (después de cargar el FXML).
     * Configura el estilo inicial del mensaje.
     */
    @FXML
    public void initialize() {
        messageLabel.getStyleClass().setAll("lbl-info"); // Estilo por defecto Bootstrap: informativo
    }

    /**
     * Maneja el evento de clic en el botón "Iniciar Sesión".
     */
    @FXML
    protected void onLoginButtonClick() {
        final String username = usernameField.getText();
        final String password = passwordField.getText();

        // 1. Deshabilitar la UI y mostrar estado de carga
        setControlsDisabled(true);
        messageLabel.setText("Verificando credenciales...");
        messageLabel.getStyleClass().setAll("lbl-warning"); // Estilo de advertencia/proceso

        // 2. Simular la llamada al Backend en un hilo secundario
        executor.schedule(() -> {

            // --- INICIO DE LA SIMULACIÓN DE BACKEND (MOCKING) ---
            // Credenciales simuladas: admin / 123
            boolean success = "admin".equals(username) && "123".equals(password);
            // --- FIN DE LA SIMULACIÓN ---

            // 3. Actualizar la UI de vuelta en el hilo de JavaFX
            Platform.runLater(() -> {
                if (success) {
                    messageLabel.setText("¡Inicio de sesión exitoso! Redirigiendo...");
                    messageLabel.getStyleClass().setAll("lbl-success"); // Estilo de éxito Bootstrap

                    try {
                        navigateToDashboard();
                    } catch (IOException e) {
                        System.err.println("Error al cargar la vista principal (Dashboard).");
                        e.printStackTrace();
                        messageLabel.setText("Error interno al cargar la app.");
                        messageLabel.getStyleClass().setAll("lbl-danger");
                        setControlsDisabled(false);
                    }

                } else {
                    messageLabel.setText("Credenciales incorrectas. Intente de nuevo.");
                    messageLabel.getStyleClass().setAll("lbl-danger"); // Estilo de error Bootstrap
                    setControlsDisabled(false); // Habilitar controles para reintento
                    passwordField.clear(); // Limpiar solo el campo de contraseña
                }
            });
        }, 2, TimeUnit.SECONDS); // Simula 2 segundos de latencia de red
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
        currentStage.setTitle("Restaurante POS - Panel Principal");
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
}
