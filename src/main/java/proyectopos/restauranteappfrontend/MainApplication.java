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
    // Cambiamos al nuevo archivo de tema
    private static final String RAIZ_IQUENA_THEME_CSS = "/proyectopos/restauranteappfrontend/raiz-iquena-theme.css";
    // --- FIN RUTA MODIFICADA ---
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/proyectopos/restauranteappfrontend/" + LOGIN_VIEW_FILE));
            Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

            // --- ESTILOS ---
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet()); // Mantenemos BootstrapFX como base

            // Cargar nuestro tema Raíz Iqueña
            URL cssUrl = MainApplication.class.getResource(RAIZ_IQUENA_THEME_CSS); // Usamos la nueva ruta
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("Tema Raíz Iqueña cargado.");
            } else {
                System.err.println("Error: No se pudo cargar raiz-iquena-theme.css desde: " + RAIZ_IQUENA_THEME_CSS);
            }
            // --- FIN ESTILOS ---

            stage.setTitle("Restaurante POS Raíz Iqueña - Iniciar Sesión"); // Título actualizado
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