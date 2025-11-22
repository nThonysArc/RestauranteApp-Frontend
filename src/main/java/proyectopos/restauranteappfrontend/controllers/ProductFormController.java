package proyectopos.restauranteappfrontend.controllers;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.MediaService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class ProductFormController {

    @FXML private TextField nombreField;
    @FXML private TextArea descripcionField;
    @FXML private TextField precioField;
    @FXML private ComboBox<CategoriaDTO> catPadreCombo;
    @FXML private ComboBox<CategoriaDTO> subCatCombo;
    @FXML private Label rutaImagenLabel;
    @FXML private Button btnSeleccionarImagen;

    // Servicios necesarios
    private final CategoriaService categoriaService = new CategoriaService();
    private final MediaService mediaService = new MediaService();

    // Estado interno
    private String urlImagenSubida = null;
    private List<CategoriaDTO> todasLasCategorias;
    private Long idProductoEdicion = null; // Si es null, es creación

    @FXML
    public void initialize() {
        // Cargar categorías en segundo plano al iniciar el formulario
        cargarCategorias();

        // Configurar lógica de categorías dependientes (Padre -> Subcategoría)
        catPadreCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            subCatCombo.getItems().clear();
            if (newVal != null && todasLasCategorias != null) {
                List<CategoriaDTO> subs = todasLasCategorias.stream()
                        .filter(c -> newVal.getIdCategoria().equals(c.getIdCategoriaPadre()))
                        .collect(Collectors.toList());
                subCatCombo.setItems(FXCollections.observableArrayList(subs));
                subCatCombo.setDisable(false);
            } else {
                subCatCombo.setDisable(true);
            }
        });
    }

    /**
     * Método para precargar datos si estamos editando un producto existente.
     */
    public void setProducto(ProductoDTO producto) {
        if (producto == null) return;

        this.idProductoEdicion = producto.getIdProducto();
        nombreField.setText(producto.getNombre());
        descripcionField.setText(producto.getDescripcion());
        precioField.setText(String.valueOf(producto.getPrecio()));
        
        // Mantener la URL actual por si no la cambia
        this.urlImagenSubida = producto.getImagenUrl();
        if (urlImagenSubida != null) {
            rutaImagenLabel.setText("Imagen actual conservada");
            rutaImagenLabel.setStyle("-fx-text-fill: green;");
        }

        // Seleccionar categorías en los combos (Esto requiere que las categorías ya estén cargadas)
        // Nota: En un caso real, podrías necesitar esperar a que cargue la lista antes de seleccionar.
        // Aquí asumimos una carga rápida o se podría mejorar con un callback.
    }

    /**
     * Devuelve un DTO con los datos del formulario listos para enviar al backend.
     * Retorna null si la validación falla.
     */
    public ProductoDTO getProductoResult() {
        try {
            String nombre = nombreField.getText();
            if (nombre.isBlank()) {
                mostrarAlerta("Nombre requerido");
                return null;
            }

            double precio = Double.parseDouble(precioField.getText());
            
            ProductoDTO dto = new ProductoDTO();
            dto.setIdProducto(idProductoEdicion); // Será null si es nuevo, ID si es edición
            dto.setNombre(nombre);
            dto.setDescripcion(descripcionField.getText());
            dto.setPrecio(precio);
            dto.setImagenUrl(urlImagenSubida);

            // Lógica para decidir el ID de categoría final
            if (subCatCombo.getValue() != null) {
                dto.setIdCategoria(subCatCombo.getValue().getIdCategoria());
            } else if (catPadreCombo.getValue() != null) {
                dto.setIdCategoria(catPadreCombo.getValue().getIdCategoria());
            } else {
                mostrarAlerta("Debe seleccionar una categoría");
                return null;
            }

            return dto;

        } catch (NumberFormatException e) {
            mostrarAlerta("El precio debe ser un número válido.");
            return null;
        }
    }

    @FXML
    private void handleSeleccionarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(nombreField.getScene().getWindow());

        if (file != null) {
            rutaImagenLabel.setText("Subiendo: " + file.getName() + "...");
            btnSeleccionarImagen.setDisable(true);

            // Subir en segundo plano para no congelar la UI
            ThreadManager.getInstance().execute(() -> {
                try {
                    // Usamos MediaService para subir la imagen
                    String url = mediaService.uploadImage(file);
                    
                    Platform.runLater(() -> {
                        this.urlImagenSubida = url;
                        rutaImagenLabel.setText("Subida: " + file.getName());
                        rutaImagenLabel.setStyle("-fx-text-fill: green;");
                        btnSeleccionarImagen.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        rutaImagenLabel.setText("Error al subir.");
                        rutaImagenLabel.setStyle("-fx-text-fill: red;");
                        btnSeleccionarImagen.setDisable(false);
                        e.printStackTrace();
                    });
                }
            });
        }
    }

    private void cargarCategorias() {
        ThreadManager.getInstance().execute(() -> {
            try {
                todasLasCategorias = categoriaService.getAllCategorias();
                List<CategoriaDTO> padres = todasLasCategorias.stream()
                        .filter(c -> c.getIdCategoriaPadre() == null)
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    catPadreCombo.setItems(FXCollections.observableArrayList(padres));
                    // Si estábamos editando y ya tenemos datos, aquí podríamos intentar pre-seleccionar
                    // los combos basándonos en el ID de categoría del producto.
                });
            } catch (Exception e) {
                Platform.runLater(() -> rutaImagenLabel.setText("Error cargando categorías"));
            }
        });
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}