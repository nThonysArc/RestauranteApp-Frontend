package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import proyectopos.restauranteappfrontend.MainApplication;
import proyectopos.restauranteappfrontend.util.SessionManager;

public class MainController {

    @FXML private StackPane mainContentPane;
    @FXML private Label userInfoLabel;
    @FXML private MenuBar mainMenuBar;
    @FXML private Menu adminMenu;

    // --- NUEVO: Variable para guardar el controlador actual ---
    private Object currentController = null;
    // --- FIN NUEVO ---


    @FXML
    public void initialize() {
        String userRole = SessionManager.getInstance().getRole();
        String userName = SessionManager.getInstance().getNombre();

        if (userRole == null || !SessionManager.getInstance().isAuthenticated()) {
            userInfoLabel.setText("Usuario: SESIÓN INVÁLIDA");
            if (adminMenu != null) adminMenu.setVisible(false);
            handleCerrarSesion();
            return;
        }

        userInfoLabel.setText("Usuario: " + userName + " (" + userRole.replace("ROLE_", "") + ")");

        // Mostrar/Ocultar Menú Admin
        if (adminMenu != null) {
            adminMenu.setVisible("ROLE_ADMIN".equals(userRole));
        } else {
             System.err.println("Advertencia: No se pudo encontrar el Menu 'adminMenu'.");
        }

        // Carga inicial de vista según rol
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
                mainContentPane.getChildren().add(new Label("Error: Rol no reconocido."));
                break;
        }
    }

    /**
     * Carga un archivo FXML en el panel central (mainContentPane).
     * Antes de cargar la nueva vista, llama a cleanup() en el controlador anterior si existe.
     * @param fxmlFile El nombre del archivo FXML (ej. "dashboard-view.fxml")
     */
    // --- ¡¡MÉTODO MODIFICADO PARA LLAMAR A CLEANUP!! ---
    private void loadView(String fxmlFile) {
        try {
            // 1. Limpiar el controlador anterior si es necesario
            if (currentController != null && currentController instanceof CleanableController) {
                System.out.println("Llamando cleanup() en: " + currentController.getClass().getSimpleName());
                ((CleanableController) currentController).cleanup();
            }

            // 2. Cargar la nueva vista y su controlador
            String fxmlPath = "/proyectopos/restauranteappfrontend/" + fxmlFile;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // 3. Obtener y guardar la referencia al NUEVO controlador
            currentController = loader.getController();
            System.out.println("Vista cargada: " + fxmlFile + ", Controlador: " + (currentController != null ? currentController.getClass().getSimpleName() : "null"));

            // 4. Mostrar la nueva vista
            mainContentPane.getChildren().clear();
            mainContentPane.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
            // Limpiar controlador actual en caso de error de carga
            currentController = null;
            mainContentPane.getChildren().clear();
            mainContentPane.getChildren().add(new Label("Error al cargar la vista: " + fxmlFile));
            // Mostrar alerta al usuario
             new Alert(Alert.AlertType.ERROR, "No se pudo cargar la vista: " + fxmlFile + "\n" + e.getMessage()).showAndWait();
        } catch (Exception e) {
             // Captura genérica para otros posibles errores durante la carga o cleanup
             e.printStackTrace();
             currentController = null;
             mainContentPane.getChildren().clear();
             mainContentPane.getChildren().add(new Label("Error inesperado al cargar la vista."));
             new Alert(Alert.AlertType.ERROR, "Ocurrió un error inesperado al cargar la vista:\n" + e.getMessage()).showAndWait();
        }
    }
    // --- FIN MÉTODO MODIFICADO ---


    // --- Métodos para los Menús (sin cambios en su lógica interna, solo llaman a loadView) ---

    @FXML private void handleShowDashboard() { loadView("dashboard-view.fxml"); }
    @FXML private void handleShowCocina() { loadView("kitchen-view.fxml"); }
    @FXML private void handleShowCaja() { loadView("cashier-view.fxml"); }
    @FXML private void handleShowEmployeeManagement() {
        if ("ROLE_ADMIN".equals(SessionManager.getInstance().getRole())) {
            loadView("employee-management-view.fxml");
        } else {
             new Alert(Alert.AlertType.WARNING, "Acceso denegado.").show();
        }
    }

    @FXML
    private void handleCerrarSesion() {
        // --- AÑADIDO: Asegurarse de limpiar el controlador actual antes de cerrar sesión ---
        if (currentController != null && currentController instanceof CleanableController) {
             try {
                  System.out.println("Llamando cleanup() antes de cerrar sesión en: " + currentController.getClass().getSimpleName());
                 ((CleanableController) currentController).cleanup();
             } catch (Exception e) {
                  System.err.println("Error durante cleanup al cerrar sesión: " + e.getMessage());
                  e.printStackTrace();
             }
             currentController = null; // Limpiar referencia
        }
        // --- FIN AÑADIDO ---


        // Lógica existente para volver al login
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
            new Alert(Alert.AlertType.ERROR, "Error al volver al login.").show();
        }
    }
}