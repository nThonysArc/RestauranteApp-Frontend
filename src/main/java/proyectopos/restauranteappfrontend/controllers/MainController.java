package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu; // <-- AÑADIR IMPORT
import javafx.scene.control.MenuBar; // <-- AÑADIR IMPORT
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import proyectopos.restauranteappfrontend.MainApplication;
import proyectopos.restauranteappfrontend.util.SessionManager;

public class MainController {

    @FXML private StackPane mainContentPane;
    @FXML private Label userInfoLabel;
    @FXML private MenuBar mainMenuBar; // <-- AÑADIR FXML ID
    @FXML private Menu adminMenu;     // <-- AÑADIR FXML ID

    @FXML
    public void initialize() {
        String userRole = SessionManager.getInstance().getRole();
        String userName = SessionManager.getInstance().getNombre();

        if (userRole == null || !SessionManager.getInstance().isAuthenticated()) {
            userInfoLabel.setText("Usuario: SESIÓN INVÁLIDA");
            if (mainMenuBar != null && adminMenu != null) { // Asegurarse que existen antes de ocultar
                 adminMenu.setVisible(false); // Ocultar menú admin si la sesión es inválida
            }
            handleCerrarSesion(); 
            return;
        }
        
        userInfoLabel.setText("Usuario: " + userName + " (" + userRole.replace("ROLE_", "") + ")");

        // === LÓGICA PARA MOSTRAR/OCULTAR MENÚ ADMIN ===
        if (adminMenu != null) { // Comprobar si el FXML ID fue inyectado
            if ("ROLE_ADMIN".equals(userRole)) {
                adminMenu.setVisible(true); // Mostrar si es Admin
            } else {
                adminMenu.setVisible(false); // Ocultar si no es Admin
            }
        } else {
             System.err.println("Advertencia: No se pudo encontrar el Menu 'adminMenu' en MainController.");
        }
        // === FIN LÓGICA MENÚ ADMIN ===


        // Lógica del enrutador (¡Ahora es dinámica!)
        switch (userRole) {
            case "ROLE_ADMIN":
            case "ROLE_MESERO":
                loadView("dashboard-view.fxml");
                break;
            case "ROLE_COCINA":
                loadView("kitchen-view.fxml");
                break;
            case "ROLE_CAJERO":
                loadView("cashier-view.fxml");
                break;
            default:
                mainContentPane.getChildren().clear();
                mainContentPane.getChildren().add(new Label("Error: Rol de usuario no reconocido."));
                break;
        }
    }

    /**
     * Carga un archivo FXML en el panel central (mainContentPane).
     * @param fxmlFile El nombre del archivo FXML (ej. "dashboard-view.fxml")
     */
    private void loadView(String fxmlFile) {
        // ... (Este método no cambia)
        try {
            String fxmlPath = "/proyectopos/restauranteappfrontend/" + fxmlFile;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainContentPane.getChildren().clear();
            mainContentPane.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            mainContentPane.getChildren().clear();
            mainContentPane.getChildren().add(new Label("Error al cargar la vista: " + fxmlFile));
        }
    }

    // --- Métodos para los Menús ---

    @FXML
    private void handleShowDashboard() {
        loadView("dashboard-view.fxml");
    }

    @FXML
    private void handleShowCocina() {
        loadView("kitchen-view.fxml");
    }

    @FXML
    private void handleShowCaja() {
        loadView("cashier-view.fxml");
    }
    
    // === NUEVO MÉTODO PARA CARGAR VISTA DE EMPLEADOS ===
    @FXML
    private void handleShowEmployeeManagement() {
        // Verificar rol aquí también por seguridad, aunque el menú esté oculto
        if ("ROLE_ADMIN".equals(SessionManager.getInstance().getRole())) {
            loadView("employee-management-view.fxml");
        } else {
             // Opcional: Mostrar alerta si alguien intenta acceder sin permiso (no debería pasar si el menú está oculto)
             new Alert(Alert.AlertType.WARNING, "Acceso denegado a la gestión de empleados.").show();
        }
    }
    // === FIN NUEVO MÉTODO ===


    @FXML
    private void handleCerrarSesion() {
        // ... (Este método no cambia)
        SessionManager.getInstance().clearSession();
        try {
            Stage stage = (Stage) mainContentPane.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/proyectopos/restauranteappfrontend/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600); 
            scene.getStylesheets().add(org.kordamp.bootstrapfx.BootstrapFX.bootstrapFXStylesheet());
            scene.getStylesheets().add(MainApplication.class.getResource("/proyectopos/restauranteappfrontend/dark-theme.css").toExternalForm());
            stage.setTitle("Restaurante POS - Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al intentar cerrar sesión y volver al login.").show();
        }
    }
}