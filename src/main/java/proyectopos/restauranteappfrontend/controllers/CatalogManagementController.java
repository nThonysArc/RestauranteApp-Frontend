package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.DataCacheService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class CatalogManagementController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final Runnable onDataChangedCallback; // Callback para refrescar el Dashboard

    public CatalogManagementController(Runnable onDataChangedCallback) {
        this.productoService = new ProductoService();
        this.categoriaService = new CategoriaService();
        this.onDataChangedCallback = onDataChangedCallback;
    }

    // --- GESTIÓN DE PRODUCTOS ---

    public void crearProducto() {
        abrirDialogoProducto(null, "Nuevo Producto");
    }

    public void editarProducto(ProductoDTO producto) {
        abrirDialogoProducto(producto, "Editar Producto");
    }

    public void eliminarProducto(ProductoDTO producto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar");
        alert.setHeaderText("¿Eliminar " + producto.getNombre() + "?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            ThreadManager.getInstance().execute(() -> {
                try {
                    productoService.eliminarProducto(producto.getIdProducto());
                    notifySuccess("Producto eliminado.");
                } catch (Exception e) {
                    notifyError("Error al eliminar producto", e);
                }
            });
        }
    }

    // Lógica compartida para el diálogo (Crear/Editar)
    private void abrirDialogoProducto(ProductoDTO producto, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/product-form-view.fxml"));
            Parent formView = loader.load();
            ProductFormController controller = loader.getController();

            if (producto != null) {
                controller.setProducto(producto);
            }

            Dialog<ProductoDTO> dialog = new Dialog<>();
            dialog.setTitle(titulo);
            dialog.getDialogPane().setContent(formView);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(bt -> bt == ButtonType.OK ? controller.getProductoResult() : null);

            Optional<ProductoDTO> result = dialog.showAndWait();
            result.ifPresent(this::guardarProductoAsync);

        } catch (IOException e) {
            notifyError("Error al abrir el formulario", e);
        }
    }

    private void guardarProductoAsync(ProductoDTO dto) {
        ThreadManager.getInstance().execute(() -> {
            try {
                if (dto.getIdProducto() == null) {
                    productoService.crearProducto(dto);
                } else {
                    productoService.actualizarProducto(dto.getIdProducto(), dto);
                }
                notifySuccess("Producto guardado correctamente.");
            } catch (Exception e) {
                notifyError("Error al guardar producto", e);
            }
        });
    }

    // --- GESTIÓN DE CATEGORÍAS ---

    public void gestionarCategorias() {
        // Primero cargamos las categorías actuales para saber cuáles pueden ser padres
        ThreadManager.getInstance().execute(() -> {
            try {
                List<CategoriaDTO> todas = categoriaService.getAllCategorias();
                // Filtramos solo las que no tienen padre (categorías raíz)
                List<CategoriaDTO> padres = todas.stream()
                        .filter(c -> c.getIdCategoriaPadre() == null)
                        .collect(Collectors.toList());

                Platform.runLater(() -> mostrarDialogoCategoria(padres));
            } catch (Exception e) {
                notifyError("Error al cargar categorías", e);
            }
        });
    }

    private void mostrarDialogoCategoria(List<CategoriaDTO> padresDisponibles) {
        Dialog<CategoriaDTO> dialog = new Dialog<>();
        dialog.setTitle("Categorías");
        dialog.setHeaderText("Nueva Categoría");
        
        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre");
        
        ComboBox<CategoriaDTO> categoriaPadreComboBox = new ComboBox<>();
        categoriaPadreComboBox.setItems(FXCollections.observableArrayList(padresDisponibles));
        categoriaPadreComboBox.setPromptText("Categoría padre (Opcional)");
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Padre:"), 0, 1);
        grid.add(categoriaPadreComboBox, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(nombreField::requestFocus);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                CategoriaDTO nuevaCategoria = new CategoriaDTO();
                nuevaCategoria.setNombre(nombreField.getText().trim());
                CategoriaDTO padreSeleccionado = categoriaPadreComboBox.getValue();
                nuevaCategoria.setIdCategoriaPadre(padreSeleccionado != null ? padreSeleccionado.getIdCategoria() : null);
                return nuevaCategoria;
            }
            return null;
        });
        
        Optional<CategoriaDTO> result = dialog.showAndWait();
        result.ifPresent(this::guardarCategoriaAsync);
    }

    private void guardarCategoriaAsync(CategoriaDTO categoria) {
        ThreadManager.getInstance().execute(() -> {
            try {
                categoriaService.crearCategoria(categoria);
                notifySuccess("Categoría creada correctamente.");
            } catch (Exception e) {
                notifyError("Error al crear categoría", e);
            }
        });
    }

    // --- UTILIDADES ---

    private void notifySuccess(String mensaje) {
        Platform.runLater(() -> {
            DataCacheService.getInstance().limpiarCache(); // Limpiar caché local
            if (onDataChangedCallback != null) {
                onDataChangedCallback.run(); // ¡Avisar al Dashboard!
            }
            new Alert(Alert.AlertType.INFORMATION, mensaje).show();
        });
    }

    private void notifyError(String titulo, Exception e) {
        Platform.runLater(() -> {
            System.err.println(titulo + ": " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, titulo + "\n" + e.getMessage()).show();
        });
    }
}