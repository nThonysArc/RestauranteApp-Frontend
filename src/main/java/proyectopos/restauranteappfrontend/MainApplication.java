package proyectopos.restauranteappfrontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX; // Asegúrate que está importado

import java.io.IOException;
import java.net.URL; // ⬅️ AÑADIDO

public class MainApplication extends Application {

    private static final String LOGIN_VIEW_FILE = "login-view.fxml";
    private static final String DARK_THEME_CSS = "dark-theme.css"; // ⬅️ AÑADIDO
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(LOGIN_VIEW_FILE));
            Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

            // --- ESTILOS (MODIFICADO) ---
            // 1. Cargar BootstrapFX (como ya lo hacías)
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

            // 2. Cargar nuestro tema oscuro DESPUÉS
            URL cssUrl = MainApplication.class.getResource(DARK_THEME_CSS);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("Dark theme loaded successfully."); // Mensaje de depuración
            } else {
                System.err.println("Warning: Could not load dark theme CSS (" + DARK_THEME_CSS + ")");
            }
            // --- FIN ESTILOS ---

            stage.setTitle("Restaurante POS - Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            System.err.println("Error al cargar la vista FXML (" + LOGIN_VIEW_FILE + "): " + e.getMessage());
            e.printStackTrace();
            throw e; // Relanzar para que se vea el error
        } catch (Exception e) {
            System.err.println("Error al iniciar la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}