package proyectopos.restauranteappfrontend.controllers;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.ImageCacheService;
import proyectopos.restauranteappfrontend.util.AppConfig;
import proyectopos.restauranteappfrontend.util.SessionManager;

public class ProductoCardController {

    @FXML private VBox cardContainer;
    @FXML private ImageView productoImage;
    @FXML private Label nombreLabel;
    @FXML private Label precioLabel;

    private ProductoDTO producto;

    public void setData(ProductoDTO producto, 
                        Consumer<ProductoDTO> onSelect, 
                        Consumer<ProductoDTO> onEdit, 
                        Consumer<ProductoDTO> onDelete) {
        this.producto = producto;

        // 1. Setear Textos
        nombreLabel.setText(producto.getNombre());
        precioLabel.setText(String.format("S/ %.2f", producto.getPrecio()));

        // 2. Cargar Imagen (Usando tu ImageCacheService)
        cargarImagen(producto.getImagenUrl());

        // 3. Configurar Clic (Agregar al pedido)
        cardContainer.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                onSelect.accept(producto);
                efectoClic();
            }
        });

        // 4. Configurar Menú Contextual (Solo Admin)
        if ("ROLE_ADMIN".equals(SessionManager.getInstance().getRole())) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem itemEditar = new MenuItem("Editar Producto");
            itemEditar.setOnAction(e -> onEdit.accept(producto));

            MenuItem itemEliminar = new MenuItem("Eliminar Producto");
            itemEliminar.setStyle("-fx-text-fill: red;");
            itemEliminar.setOnAction(e -> onDelete.accept(producto));

            contextMenu.getItems().addAll(itemEditar, itemEliminar);
            
            cardContainer.setOnContextMenuRequested(e -> 
                contextMenu.show(cardContainer, e.getScreenX(), e.getScreenY())
            );
        }
    }

    private void cargarImagen(String url) {
        try {
            String urlFinal;
            if (url != null && !url.isBlank()) {
                if (url.startsWith("http") || url.startsWith("file:")) {
                    urlFinal = url;
                } else {
                    urlFinal = AppConfig.getInstance().getApiBaseUrl() + url;
                }
            } else {
                urlFinal = "https://via.placeholder.com/150?text=" + producto.getNombre().replace(" ", "+");
            }
            Image image = ImageCacheService.getInstance().getImage(urlFinal);
            productoImage.setImage(image);
        } catch (Exception e) {
            System.err.println("Error imagen: " + e.getMessage());
        }
    }

    private void efectoClic() {
        cardContainer.setOpacity(0.5);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() { 
                javafx.application.Platform.runLater(() -> cardContainer.setOpacity(1.0)); 
            }
        }, 100);
    }
}