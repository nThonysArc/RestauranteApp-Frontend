package proyectopos.restauranteappfrontend;

import java.io.IOException;
import java.net.URL;

import org.kordamp.bootstrapfx.BootstrapFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import proyectopos.restauranteappfrontend.services.WebSocketService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class MainApplication extends Application {

    private static final String LOGIN_VIEW_FILE = "login-view.fxml";
    private static final String RAIZ_IQUENA_THEME_CSS = "/proyectopos/restauranteappfrontend/raiz-iquena-theme.css";
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/proyectopos/restauranteappfrontend/" + LOGIN_VIEW_FILE));
            Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet()); 
            URL cssUrl = MainApplication.class.getResource(RAIZ_IQUENA_THEME_CSS); 
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("Tema Raíz Iqueña cargado.");
            } else {
                System.err.println("Error: No se pudo cargar raiz-iquena-theme.css desde: " + RAIZ_IQUENA_THEME_CSS);
            }

            stage.setTitle("Restaurante POS Raíz Iqueña - Iniciar Sesión"); 
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

    @Override
    public void stop() throws Exception {
        System.out.println("Deteniendo aplicación...");
        
        // 1. Desconectar WebSocket
        WebSocketService.getInstance().disconnect();
        
        // 2. Apagar el pool de hilos
        ThreadManager.getInstance().shutdown();
        
        super.stop();
    }
}