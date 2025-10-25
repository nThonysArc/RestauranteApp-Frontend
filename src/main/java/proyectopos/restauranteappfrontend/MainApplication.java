package proyectopos.restauranteappfrontend;

import java.io.IOException;
import java.net.URL;

import org.kordamp.bootstrapfx.BootstrapFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private static final String LOGIN_VIEW_FILE = "login-view.fxml";
    // --- RUTA MODIFICADA ---
    // Añadimos la ruta completa desde la raíz de resources (classpath)
    private static final String DARK_THEME_CSS = "/proyectopos/restauranteappfrontend/dark-theme.css"; 
    // --- FIN RUTA MODIFICADA ---
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // --- CORRECCIÓN AL CARGAR FXML ---
            // Usar getResource con la ruta absoluta también para FXML por consistencia
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/proyectopos/restauranteappfrontend/" + LOGIN_VIEW_FILE));
            // --- FIN CORRECCIÓN FXML ---
            
            Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

            // --- ESTILOS ---
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

            // Cargar nuestro tema oscuro
            URL cssUrl = MainApplication.class.getResource(DARK_THEME_CSS); // Usamos la ruta modificada
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("Dark theme loaded successfully."); 
            } else {
                // Mensaje de error más específico
                System.err.println("Error: Could not load dark theme CSS from path: " + DARK_THEME_CSS);
            }
            // --- FIN ESTILOS ---

            stage.setTitle("Restaurante POS - Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            System.err.println("Error al cargar la vista FXML (" + LOGIN_VIEW_FILE + "): " + e.getMessage());
            e.printStackTrace();
            throw e; 
        } catch (Exception e) {
            System.err.println("Error al iniciar la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}