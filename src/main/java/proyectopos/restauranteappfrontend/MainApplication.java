package proyectopos.restauranteappfrontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX; // Importación necesaria para el estilo

import java.io.IOException;

public class MainApplication extends Application {

    // Nombre del archivo FXML
    private static final String LOGIN_VIEW_FILE = "login-view.fxml";
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // 1. Cargar el FXML de login.
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(LOGIN_VIEW_FILE));

            // 2. Configurar la escena con el tamaño deseado.
            Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

            // 3. APLICAR BOOTSTRAPFX CSS
            // Esto carga los estilos de la librería para dar una apariencia moderna.
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

            // 4. Configurar y mostrar la ventana (Stage)
            stage.setTitle("Restaurante POS - Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false); // La ventana de login no debería ser redimensionable
            stage.show();

        } catch (IOException e) {
            // Manejo de errores si el archivo FXML no se encuentra o no se puede cargar
            System.err.println("Error al cargar la vista FXML (" + LOGIN_VIEW_FILE + "): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error al iniciar la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
