package proyectopos.restauranteappfrontend.controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu; 
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem; 
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.model.dto.WebSocketMessageDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.DataCacheService; // IMPORTADO
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.ImageCacheService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.services.WebSocketService;
import proyectopos.restauranteappfrontend.util.AppConfig;
import proyectopos.restauranteappfrontend.util.SessionManager;
import proyectopos.restauranteappfrontend.util.ThreadManager;
import javafx.scene.control.ScrollPane;

public class DashboardController implements CleanableController {

    @FXML private Label infoLabel;
    @FXML private TilePane mesasContainer;
    @FXML private VBox gestionPedidoPane;
    @FXML private ListView<CategoriaDTO> categoriasListView;
    @FXML private ListView<CategoriaDTO> subCategoriasListView;

    @FXML private TilePane productosContainer;

    @FXML private Label mesaSeleccionadaLabel;
    @FXML private TableView<DetallePedidoMesaDTO> pedidoActualTableView;
    @FXML private TableColumn<DetallePedidoMesaDTO, String> pedidoNombreCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Integer> pedidoCantidadCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoPrecioCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoSubtotalCol;
    @FXML private Label subTotalPedidoLabel;
    @FXML private Label igvPedidoLabel;
    @FXML private Label totalPedidoLabel;
    @FXML private Button crearPedidoButton;

    private HBox adminButtonContainer = null;

    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    private final Gson gson = new Gson();

    private MesaDTO mesaSeleccionada = null;
    private PedidoMesaDTO pedidoActual = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();

    private final ObservableList<DetallePedidoMesaDTO> itemsCompletosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> itemsEnviadosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> itemsNuevosData = FXCollections.observableArrayList();

    private final ObservableList<CategoriaDTO> categoriasData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> subCategoriasData = FXCollections.observableArrayList();
    private FilteredList<ProductoDTO> filteredProductos;
    private Map<Long, String> estadoPedidoCache = new HashMap<>();


    @FXML
    public void initialize() {
        infoLabel.setText("Cargando sistema...");
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");

        // Estado inicial: Vista de Mesas activa, Vista de Pedido oculta
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);

        categoriasListView.setPlaceholder(new Label("Cargando categorías..."));
        subCategoriasListView.setPlaceholder(new Label("Seleccione categoría"));

        if(productosContainer != null) {
            productosContainer.getChildren().clear();
            productosContainer.getChildren().add(new Label("Cargando productos..."));
        }

        mesasContainer.getChildren().clear();
        mesasContainer.getChildren().add(new Label("Cargando mesas..."));

        configurarContenedorMesas();
        configurarTablaPedidoActual();
        cargarDatosIniciales();

        crearPedidoButton.setDisable(true);

        subCategoriasListView.setItems(subCategoriasData);

        // Listener Categorías
        categoriasListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, categoriaSeleccionada) -> {
                    subCategoriasData.clear();
                    subCategoriasListView.getSelectionModel().clearSelection();

                    if (categoriaSeleccionada != null) {
                        infoLabel.setText("Categoría: " + categoriaSeleccionada.getNombre());
                        List<CategoriaDTO> subcategorias = categoriasData.stream()
                                .filter(c -> categoriaSeleccionada.getIdCategoria().equals(c.getIdCategoriaPadre()))
                                .collect(Collectors.toList());
                        subCategoriasData.addAll(subcategorias);
                        subCategoriasListView.setPlaceholder(new Label(subcategorias.isEmpty() ? "Sin subcategorías" : "Seleccione..."));

                        if (filteredProductos != null) {
                            Set<Long> idsSubcategorias = subcategorias.stream().map(CategoriaDTO::getIdCategoria).collect(Collectors.toSet());
                            if (idsSubcategorias.isEmpty()) {
                                filteredProductos.setPredicate(p -> categoriaSeleccionada.getIdCategoria().equals(p.getIdCategoria()));
                            } else {
                                filteredProductos.setPredicate(p -> idsSubcategorias.contains(p.getIdCategoria()));
                            }
                        }
                    } else {
                        subCategoriasListView.setPlaceholder(new Label("Seleccione categoría principal"));
                        if (filteredProductos != null) {
                            filteredProductos.setPredicate(p -> true);
                        }
                        infoLabel.setText("Seleccione una categoría");
                    }
                    renderizarProductos();
                }
        );

        // Listener SubCategorías
        subCategoriasListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, subCategoriaSeleccionada) -> {
                    if (filteredProductos == null) return;
                    if (subCategoriaSeleccionada != null) {
                        infoLabel.setText("Subcategoría: " + subCategoriaSeleccionada.getNombre());
                        filteredProductos.setPredicate(producto -> producto.getIdCategoria().equals(subCategoriaSeleccionada.getIdCategoria()));
                    } else {
                        // Si se deselecciona subcategoría, volvemos a la categoría padre
                        CategoriaDTO catPrincipal = categoriasListView.getSelectionModel().getSelectedItem();
                        if (catPrincipal != null) {
                            infoLabel.setText("Categoría: " + catPrincipal.getNombre());
                            List<CategoriaDTO> subcategorias = categoriasData.stream().filter(c -> catPrincipal.getIdCategoria().equals(c.getIdCategoriaPadre())).collect(Collectors.toList());
                            Set<Long> idsSubcategorias = subcategorias.stream().map(CategoriaDTO::getIdCategoria).collect(Collectors.toSet());
                            if (idsSubcategorias.isEmpty()) {
                                filteredProductos.setPredicate(p -> catPrincipal.getIdCategoria().equals(p.getIdCategoria()));
                            } else {
                                filteredProductos.setPredicate(p -> idsSubcategorias.contains(p.getIdCategoria()));
                            }
                        } else {
                            filteredProductos.setPredicate(p -> true);
                            infoLabel.setText("Seleccione una categoría");
                        }
                    }
                    renderizarProductos();
                }
        );

        WebSocketService.getInstance().subscribe("/topic/pedidos", (jsonMessage) -> {
            Platform.runLater(() -> procesarMensajeWebSocket(jsonMessage));
        });
    }

    // --- NUEVO MÉTODO: Volver a la vista de mesas ---
    @FXML
    private void handleVolverMesas() {
        // Ocultar panel de pedidos
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);
        
        // Limpiar selección
        this.mesaSeleccionada = null;
        this.pedidoActual = null;
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
        
        // Refrescar mesas (para ver estados actualizados si hubo cambios)
        cargarSoloMesasAsync();
        
        infoLabel.setText("Seleccione una mesa.");
    }

    private void renderizarProductos() {
        if (productosContainer == null) return;

        productosContainer.getChildren().clear();

        if (filteredProductos != null && !filteredProductos.isEmpty()) {
            for (ProductoDTO p : filteredProductos) {
                productosContainer.getChildren().add(crearTarjetaProducto(p));
            }
        } else {
            Label emptyLabel = new Label("No hay productos en esta categoría.");
            emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-padding: 20;");
            productosContainer.getChildren().add(emptyLabel);
        }
    }

    // --- DISEÑO TARJETA DE PRODUCTO ACTUALIZADO ---
    private Node crearTarjetaProducto(ProductoDTO producto) {
        // Contenedor principal (Tarjeta) con estilo nuevo
        VBox card = new VBox(10);
        card.getStyleClass().add("producto-card"); // Usamos la nueva clase CSS
        card.setPrefSize(180, 240); // Tamaño ajustado para imagen vertical
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));

        // Imagen
        ImageView imageView = new ImageView();
        imageView.setFitHeight(120);
        imageView.setFitWidth(120);
        imageView.setPreserveRatio(true);

        try {
            String urlImagen = producto.getImagenUrl();
            String urlFinal = null;

            if (urlImagen != null && !urlImagen.isBlank()) {
                if (urlImagen.startsWith("http") || urlImagen.startsWith("file:")) {
                    urlFinal = urlImagen;
                } else {
                    urlFinal = AppConfig.getInstance().getApiBaseUrl() + urlImagen;
                }
            } else {
                urlFinal = "https://via.placeholder.com/150?text=" + producto.getNombre().replace(" ", "+");
            }

            if (urlFinal != null) {
                // Pide al caché. Si ya existe, es instantáneo.
                Image imagen = ImageCacheService.getInstance().getImage(urlFinal);
                imageView.setImage(imagen);
            }

        } catch (Exception e) {
            System.err.println("No se pudo cargar imagen para: " + producto.getNombre());
        }

        // Nombre del producto
        Label lblNombre = new Label(producto.getNombre());
        lblNombre.getStyleClass().add("producto-titulo"); // Clase CSS para tipografía
        lblNombre.setWrapText(true);
        lblNombre.setAlignment(Pos.CENTER);
        lblNombre.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lblNombre.setMaxHeight(40);

        // Precio
        Label lblPrecio = new Label(String.format("S/ %.2f", producto.getPrecio()));
        lblPrecio.getStyleClass().add("precio-label"); // Clase CSS para precio destacado

        // --- Lógica de Clic Principal (Añadir al pedido) ---
        card.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                handleSeleccionarProducto(producto);
                // Efecto visual de clic
                card.setOpacity(0.5);
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override public void run() { Platform.runLater(() -> card.setOpacity(1.0)); }
                }, 100);
            }
        });

        // --- Lógica para el Menú Contextual (ADMIN) ---
        String userRole = SessionManager.getInstance().getRole();
        if ("ROLE_ADMIN".equals(userRole)) {
            ContextMenu contextMenu = new ContextMenu();

            MenuItem itemEditar = new MenuItem("Editar Producto");
            itemEditar.setOnAction(e -> handleEditarProducto(producto));

            MenuItem itemEliminar = new MenuItem("Eliminar Producto");
            itemEliminar.setStyle("-fx-text-fill: red;");
            itemEliminar.setOnAction(e -> handleEliminarProducto(producto));

            contextMenu.getItems().addAll(itemEditar, itemEliminar);

            card.setOnContextMenuRequested(e -> 
                contextMenu.show(card, e.getScreenX(), e.getScreenY())
            );
        }

        card.getChildren().addAll(imageView, lblNombre, lblPrecio);
        return card;
    }

    // --- Edición de Producto ---
    private void handleEditarProducto(ProductoDTO producto) {
        Dialog<ProductoDTO> dialog = new Dialog<>();
        dialog.setTitle("Editar Producto");
        dialog.setHeaderText("Modificar: " + producto.getNombre());

        ButtonType guardarButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 50, 10, 10));

        TextField nombreField = new TextField(producto.getNombre());
        TextArea descripcionField = new TextArea(producto.getDescripcion());
        descripcionField.setWrapText(true); descripcionField.setPrefRowCount(3);
        TextField precioField = new TextField(String.valueOf(producto.getPrecio()));

        Label lblImagen = new Label("Imagen:");
        Button btnSeleccionarImagen = new Button("Cambiar imagen...");
        btnSeleccionarImagen.getStyleClass().add("btn-secondary");
        Label lblRutaImagen = new Label(producto.getImagenUrl() != null ? "Imagen actual conservada" : "Sin imagen");
        lblRutaImagen.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
        
        final String[] newImageUrl = {null};

        btnSeleccionarImagen.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Cambiar Imagen");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
            File file = fileChooser.showOpenDialog(btnSeleccionarImagen.getScene().getWindow());
            
            if (file != null) {
                lblRutaImagen.setText("Subiendo: " + file.getName() + "...");
                btnSeleccionarImagen.setDisable(true);
                
                ThreadManager.getInstance().execute(() -> {
                    try {
                        String uploadedUrl = subirImagenAlServidor(file); 
                        Platform.runLater(() -> {
                            lblRutaImagen.setText("Nueva: " + file.getName());
                            lblRutaImagen.setStyle("-fx-text-fill: green;");
                            newImageUrl[0] = uploadedUrl;
                            btnSeleccionarImagen.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            lblRutaImagen.setText("Error al subir.");
                            mostrarAlertaError("Error", "Fallo la subida: " + e.getMessage());
                            btnSeleccionarImagen.setDisable(false);
                        });
                    }
                });
            }
        });

        grid.add(new Label("Nombre:"), 0, 0); grid.add(nombreField, 1, 0);
        grid.add(new Label("Descrip:"), 0, 1); grid.add(descripcionField, 1, 1);
        grid.add(new Label("Precio:"), 0, 2); grid.add(precioField, 1, 2);
        grid.add(lblImagen, 0, 3); 
        VBox imgBox = new VBox(5, btnSeleccionarImagen, lblRutaImagen);
        grid.add(imgBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButtonType) {
                try {
                    ProductoDTO dto = new ProductoDTO();
                    dto.setIdProducto(producto.getIdProducto()); 
                    dto.setNombre(nombreField.getText());
                    dto.setDescripcion(descripcionField.getText());
                    dto.setPrecio(Double.parseDouble(precioField.getText()));
                    dto.setIdCategoria(producto.getIdCategoria()); 
                    
                    if (newImageUrl[0] != null) {
                        dto.setImagenUrl(newImageUrl[0]);
                    } else {
                        dto.setImagenUrl(producto.getImagenUrl());
                    }
                    return dto;
                } catch (Exception e) { return null; }
            }
            return null;
        });

        Optional<ProductoDTO> result = dialog.showAndWait();
        result.ifPresent(productoEditado -> {
            infoLabel.setText("Actualizando...");
            ThreadManager.getInstance().execute(() -> {
                try {
                    productoService.actualizarProducto(productoEditado.getIdProducto(), productoEditado);
                    Platform.runLater(() -> {
                        infoLabel.setText("Producto actualizado.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        
                        // --- IMPORTANTE: Limpiar caché para forzar recarga con datos nuevos ---
                        DataCacheService.getInstance().limpiarCache(); 
                        
                        cargarDatosIniciales(); 
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> handleGenericError("Error al actualizar", e));
                }
            });
        });
    }

    // --- Eliminación de Producto ---
    private void handleEliminarProducto(ProductoDTO producto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Producto");
        alert.setHeaderText("¿Eliminar: " + producto.getNombre() + "?");
        alert.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            infoLabel.setText("Eliminando...");
            ThreadManager.getInstance().execute(() -> {
                try {
                    productoService.eliminarProducto(producto.getIdProducto());
                    Platform.runLater(() -> {
                        infoLabel.setText("Producto eliminado.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        
                        // --- IMPORTANTE: Limpiar caché para reflejar la eliminación ---
                        DataCacheService.getInstance().limpiarCache();
                        
                        cargarDatosIniciales();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> handleGenericError("Error al eliminar", e));
                }
            });
        }
    }

    private void procesarMensajeWebSocket(String jsonMessage) {
        try {
            WebSocketMessageDTO msg = gson.fromJson(jsonMessage, WebSocketMessageDTO.class);

            if (msg == null || msg.getType() == null) {
                if ("LISTO".equals(jsonMessage) || "CERRADO".equals(jsonMessage) || "NUEVO".equals(jsonMessage)) {
                    cargarSoloMesasAsync();
                }
                return;
            }

            PedidoMesaDTO pedido = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);

            if (pedido != null && pedido.getIdMesa() != null) {
                actualizarEstadoMesaEspecifica(msg.getType(), pedido);

                if (mesaSeleccionada != null && mesaSeleccionada.getIdMesa().equals(pedido.getIdMesa())) {
                    actualizarPanelDetalleSiCorresponde(msg.getType(), pedido);
                }
            }

        } catch (Exception e) {
            System.err.println("Error WS Dashboard: " + e.getMessage());
            cargarSoloMesasAsync();
        }
    }

    private void actualizarEstadoMesaEspecifica(String tipoEvento, PedidoMesaDTO pedido) {
        for (Node node : mesasContainer.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                MesaDTO mesaBtn = (MesaDTO) btn.getUserData();

                if (mesaBtn != null && mesaBtn.getIdMesa().equals(pedido.getIdMesa())) {
                    actualizarEstiloBotonMesa(btn, tipoEvento, mesaBtn);
                    if ("PEDIDO_CERRADO".equals(tipoEvento) || "PEDIDO_CANCELADO".equals(tipoEvento)) {
                        estadoPedidoCache.remove(mesaBtn.getIdMesa());
                    } else {
                        estadoPedidoCache.put(mesaBtn.getIdMesa(), pedido.getEstado());
                    }
                    break;
                }
            }
        }
    }

    private void actualizarEstiloBotonMesa(Button mesaButton, String tipoEvento, MesaDTO mesaDTO) {
        mesaButton.getStyleClass().removeAll("mesa-libre", "mesa-ocupada", "mesa-pagando", "mesa-reservada", "btn-secondary");
        VBox buttonContent = (VBox) mesaButton.getGraphic();
        Text estadoMesaText = (Text) buttonContent.getChildren().get(1);

        switch (tipoEvento) {
            case "PEDIDO_CREADO":
            case "PEDIDO_ACTUALIZADO":
                mesaDTO.setEstado("OCUPADA");
                mesaButton.getStyleClass().add("mesa-ocupada");
                estadoMesaText.setText("Ocupada");
                break;

            case "PEDIDO_LISTO":
                mesaDTO.setEstado("OCUPADA");
                mesaButton.getStyleClass().add("mesa-pagando");
                estadoMesaText.setText("¡LISTO!");
                break;

            case "PEDIDO_CERRADO":
            case "PEDIDO_CANCELADO":
                mesaDTO.setEstado("DISPONIBLE");
                mesaButton.getStyleClass().add("mesa-libre");
                estadoMesaText.setText("Libre");
                break;

            default:
                if ("DISPONIBLE".equals(mesaDTO.getEstado())) mesaButton.getStyleClass().add("mesa-libre");
                else if ("OCUPADA".equals(mesaDTO.getEstado())) mesaButton.getStyleClass().add("mesa-ocupada");
                break;
        }
        mesaButton.setUserData(mesaDTO);
    }

    private void actualizarPanelDetalleSiCorresponde(String tipoEvento, PedidoMesaDTO pedidoActualizado) {
        if ("PEDIDO_CERRADO".equals(tipoEvento) || "PEDIDO_CANCELADO".equals(tipoEvento)) {
            resetearPanelPedido();
            infoLabel.setText("El pedido de la Mesa " + pedidoActualizado.getNumeroMesa() + " ha sido cerrado.");
        } else {
             this.pedidoActual = pedidoActualizado;
             itemsEnviadosData.clear();
             if (pedidoActualizado.getDetalles() != null) {
                 itemsEnviadosData.addAll(pedidoActualizado.getDetalles());
             }
             actualizarListaCompletaYTotal();
             infoLabel.setText("Pedido actualizado remotamente.");
        }
    }

    private void cargarSoloMesasAsync() {
        ThreadManager.getInstance().execute(() -> {
            List<MesaDTO> mesas = null;
            List<PedidoMesaDTO> pedidosActivos = null;
            try {
                mesas = mesaService.getAllMesas();
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                pedidosActivos = todosLosPedidos.stream()
                        .filter(p -> !"CERRADO".equalsIgnoreCase(p.getEstado()) && !"CANCELADO".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());

            } catch (Exception e) {
                System.err.println("Error actualización mesas: " + e.getMessage());
            }

            final List<MesaDTO> finalMesas = mesas;
            final List<PedidoMesaDTO> finalPedidosActivos = pedidosActivos;

            Platform.runLater(() -> {
                if (finalMesas != null && finalPedidosActivos != null) {
                    estadoPedidoCache.clear();
                    for (PedidoMesaDTO pedido : finalPedidosActivos) {
                        estadoPedidoCache.put(pedido.getIdMesa(), pedido.getEstado());
                    }
                    mostrarMesas(finalMesas);
                }
            });
        });
    }

    private void configurarContenedorMesas() { /* Sin cambios */ }

    private void configurarTablaPedidoActual() {
        pedidoNombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        pedidoCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        pedidoPrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        pedidoSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        pedidoActualTableView.setItems(itemsCompletosData);
        pedidoActualTableView.setPlaceholder(new Label("Añada productos"));
    }

    private void configurarBotonesAdmin() {
        String userRole = SessionManager.getInstance().getRole();

        if (adminButtonContainer != null && adminButtonContainer.getParent() != null) {
             ((VBox)adminButtonContainer.getParent()).getChildren().remove(adminButtonContainer);
        }
        adminButtonContainer = null;

        if (userRole == null || !userRole.equals("ROLE_ADMIN")) {
            return;
        }

        Button crearProductoBtn = new Button("+ Nuevo Producto");
        crearProductoBtn.getStyleClass().addAll("btn-sm", "btn-info");
        crearProductoBtn.setOnAction(e -> handleCrearProducto());

        Button gestionarCategoriasBtn = new Button("Categorías");
        gestionarCategoriasBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
        gestionarCategoriasBtn.setOnAction(e -> handleGestionarCategorias());

        adminButtonContainer = new HBox(10, crearProductoBtn, gestionarCategoriasBtn);
        adminButtonContainer.setAlignment(Pos.CENTER_LEFT);
        adminButtonContainer.setPadding(new Insets(0, 0, 10, 0));

        // Intentamos inyectar los botones en la vista de productos (columna central)
        try {
            // En el nuevo layout, productosContainer está en ScrollPane -> VBox
            Node scrollPane = productosContainer.getParent().getParent(); 
            if (scrollPane instanceof ScrollPane && scrollPane.getParent() instanceof VBox) {
                VBox parentVBox = (VBox) scrollPane.getParent();
                if (!parentVBox.getChildren().contains(adminButtonContainer)) {
                     parentVBox.getChildren().add(1, adminButtonContainer); // index 1 (después del Label "Menú")
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo inyectar botones admin: " + e.getMessage());
        }
    }

    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        setUIDisabledDuringLoad(true);

        ThreadManager.getInstance().execute(() -> {
            List<MesaDTO> mesas = null;
            List<CategoriaDTO> categorias = null;
            List<ProductoDTO> productos = null;
            List<PedidoMesaDTO> pedidosActivosInicial = null;
            Exception errorGeneral = null;

            try { mesas = mesaService.getAllMesas(); } catch (Exception e) { errorGeneral = e; }
            try { categorias = categoriaService.getAllCategorias(); } catch (Exception e) { errorGeneral = e; }
            
            // --- MODIFICACIÓN: USO DE CACHÉ DE PRODUCTOS ---
            try {
                // Intentamos obtener del caché primero
                if (DataCacheService.getInstance().getProductos() != null) {
                    productos = DataCacheService.getInstance().getProductos();
                    System.out.println("Productos cargados desde CACHÉ (RAM).");
                } else {
                    // Si no está en caché, llamamos al backend y guardamos
                    productos = productoService.getAllProductos();
                    DataCacheService.getInstance().setProductos(productos);
                    System.out.println("Productos cargados desde BACKEND.");
                }
            } catch (Exception e) { errorGeneral = e; }
            // -----------------------------------------------

            try {
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                pedidosActivosInicial = todosLosPedidos.stream()
                        .filter(p -> !"CERRADO".equalsIgnoreCase(p.getEstado()) && !"CANCELADO".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());
            } catch (Exception e) { errorGeneral = e; }

            final List<MesaDTO> finalMesas = mesas;
            final List<CategoriaDTO> finalCategorias = categorias;
            final List<ProductoDTO> finalProductos = productos;
            final List<PedidoMesaDTO> finalPedidosActivos = pedidosActivosInicial;
            final Exception finalError = errorGeneral;

            Platform.runLater(() -> {
                if (finalError != null && finalError instanceof HttpClientService.AuthenticationException) {
                    handleAuthenticationError((HttpClientService.AuthenticationException) finalError);
                    return;
                }

                if (finalMesas != null) {
                     if (finalPedidosActivos != null) {
                        estadoPedidoCache.clear();
                        for (PedidoMesaDTO pedido : finalPedidosActivos) {
                            estadoPedidoCache.put(pedido.getIdMesa(), pedido.getEstado());
                        }
                    }
                    mostrarMesas(finalMesas);
                } 
                
                if (finalCategorias != null) {
                    mostrarCategorias(finalCategorias);
                }

                if (finalProductos != null) {
                    productosData.clear();
                    productosData.addAll(finalProductos);
                    filteredProductos = new FilteredList<>(productosData, p -> true);
                    renderizarProductos();
                }

                infoLabel.setText("Sistema listo.");
                infoLabel.getStyleClass().setAll("lbl-success");
                configurarBotonesAdmin();
                setUIDisabledDuringLoad(false);
            });
        });
    }

    private void setUIDisabledDuringLoad(boolean disabled) {
         if (mesasContainer != null) mesasContainer.setDisable(disabled);
         if (categoriasListView != null) categoriasListView.setDisable(disabled);
         if (productosContainer != null) productosContainer.setDisable(disabled);
    }

    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear();
        if (mesas != null && !mesas.isEmpty()) {
            for (MesaDTO mesa : mesas) {
                if ("BLOQUEADA".equals(mesa.getEstado())) continue;
                
                Button mesaButton = new Button();
                mesaButton.setUserData(mesa);
                mesaButton.getStyleClass().add("mesa-button");
                
                VBox buttonContent = new VBox(-2);
                buttonContent.setAlignment(Pos.CENTER);
                Text numeroMesaText = new Text(String.valueOf(mesa.getNumeroMesa()));
                numeroMesaText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
                Text estadoMesaText = new Text();
                estadoMesaText.setStyle("-fx-font-size: 10px;");
                
                String estadoPedidoParaMesa = estadoPedidoCache.get(mesa.getIdMesa());

                switch (mesa.getEstado()) {
                    case "DISPONIBLE":
                        mesaButton.getStyleClass().add("mesa-libre");
                        estadoMesaText.setText("Libre");
                        mesaButton.setOnAction(event -> handleSeleccionarMesa(mesa));
                        break;
                    case "OCUPADA":
                        if ("LISTO_PARA_ENTREGAR".equalsIgnoreCase(estadoPedidoParaMesa)) {
                             mesaButton.getStyleClass().add("mesa-pagando");
                             estadoMesaText.setText("¡LISTO!");
                        } else {
                            mesaButton.getStyleClass().add("mesa-ocupada");
                            estadoMesaText.setText("Ocupada");
                        }
                        mesaButton.setOnAction(event -> handleSeleccionarMesa(mesa));
                        break;
                    case "RESERVADA":
                        mesaButton.getStyleClass().add("mesa-reservada");
                        estadoMesaText.setText("Rsrv.");
                        mesaButton.setDisable(true);
                        break;
                    default:
                        mesaButton.getStyleClass().add("btn-secondary");
                        estadoMesaText.setText(mesa.getEstado());
                        mesaButton.setDisable(true);
                }
                
                // Ajuste de color de texto según estado (CSS maneja el resto)
                if ("LISTO_PARA_ENTREGAR".equalsIgnoreCase(estadoPedidoParaMesa)) {
                    numeroMesaText.setStyle(numeroMesaText.getStyle() + "; -fx-fill: #78350f;"); // Amber oscuro
                } 
                
                buttonContent.getChildren().addAll(numeroMesaText, estadoMesaText);
                mesaButton.setGraphic(buttonContent);
                mesasContainer.getChildren().add(mesaButton);
            }
        } else {
            mesasContainer.getChildren().add(new Label("Sin mesas."));
        }
    }

    private void handleSeleccionarMesa(MesaDTO mesa) {
        this.mesaSeleccionada = mesa;
        this.pedidoActual = null;

        // Limpiar datos de pedido anterior
        itemsEnviadosData.clear();
        itemsNuevosData.clear();
        itemsCompletosData.clear();
        actualizarListaCompletaYTotal();

        // Resetear filtros
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
            renderizarProductos();
        }

        // MOSTRAR VISTA DE PEDIDO (CAPA SUPERIOR)
        gestionPedidoPane.setVisible(true);
        gestionPedidoPane.setManaged(true);

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.pedidoActual = null;
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa());
            infoLabel.setText("Nueva orden para Mesa " + mesa.getNumeroMesa());
            actualizarEstadoCrearPedidoButton();
            pedidoActualTableView.setPlaceholder(new Label("Añada productos"));
        } else if ("OCUPADA".equals(mesa.getEstado())) {
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa());
            infoLabel.setText("Cargando pedido...");
            pedidoActualTableView.setPlaceholder(new Label("Cargando..."));

            ThreadManager.getInstance().execute(() -> {
                try {
                    PedidoMesaDTO pedidoCargado = pedidoMesaService.getPedidoActivoPorMesa(mesa.getIdMesa());
                    Platform.runLater(() -> {
                        if (pedidoCargado != null) {
                            this.pedidoActual = pedidoCargado;
                            if (pedidoCargado.getDetalles() != null) {
                                itemsEnviadosData.addAll(pedidoCargado.getDetalles());
                            }
                        } else {
                            this.pedidoActual = null;
                        }
                        pedidoActualTableView.setPlaceholder(new Label("Añada productos"));
                        actualizarListaCompletaYTotal();
                        actualizarEstadoCrearPedidoButton();
                        infoLabel.setText("Pedido cargado.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                             this.pedidoActual = null;
                             actualizarEstadoCrearPedidoButton();
                        } else {
                            mostrarAlertaError("Error", "No se pudo cargar el pedido.");
                            handleVolverMesas();
                        }
                    });
                }
            });
        } else {
            handleVolverMesas(); // Si está reservada u otro estado no manejado
        }
     }

    private void handleSeleccionarProducto(ProductoDTO producto) {
        if (mesaSeleccionada == null) return;
        
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Añadir Producto");
        dialog.setHeaderText(producto.getNombre());
        dialog.setContentText("Cantidad:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidadStr -> {
            try {
                int cantidad = Integer.parseInt(cantidadStr);
                if (cantidad > 0) {
                    Optional<DetallePedidoMesaDTO> existente = itemsNuevosData.stream()
                        .filter(d -> d.getIdProducto().equals(producto.getIdProducto()))
                        .findFirst();

                    if (existente.isPresent()) {
                        DetallePedidoMesaDTO detalle = existente.get();
                        detalle.setCantidad(detalle.getCantidad() + cantidad);
                    } else {
                        DetallePedidoMesaDTO detalle = new DetallePedidoMesaDTO();
                        detalle.setIdProducto(producto.getIdProducto());
                        detalle.setNombreProducto(producto.getNombre());
                        detalle.setCantidad(cantidad);
                        detalle.setPrecioUnitario(producto.getPrecio());
                        detalle.setEstadoDetalle("PENDIENTE");
                        itemsNuevosData.add(detalle);
                    }

                    actualizarListaCompletaYTotal();
                    actualizarEstadoCrearPedidoButton();
                    pedidoActualTableView.refresh();
                } 
            } catch (NumberFormatException e) { /* Ignorar */ }
        });
    }


    @FXML
    private void handleGestionarCategorias() {
        if (categoriasData.isEmpty()) return;
        
        Dialog<CategoriaDTO> dialog = new Dialog<>();
        dialog.setTitle("Categorías");
        dialog.setHeaderText("Nueva Categoría");
        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(); nombreField.setPromptText("Nombre");
        ComboBox<CategoriaDTO> categoriaPadreComboBox = new ComboBox<>();
        ObservableList<CategoriaDTO> categoriasPadre = categoriasData.stream()
                .filter(c -> c.getIdCategoriaPadre() == null)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        categoriaPadreComboBox.setItems(categoriasPadre);
        categoriaPadreComboBox.setPromptText("Categoría padre (Opcional)");
        
        grid.add(new Label("Nombre:"), 0, 0); grid.add(nombreField, 1, 0);
        grid.add(new Label("Padre:"), 0, 1); grid.add(categoriaPadreComboBox, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
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
        result.ifPresent(this::llamarCrearCategoriaApi);
    }

    private void llamarCrearCategoriaApi(CategoriaDTO categoriaACrear) {
        infoLabel.setText("Creando categoría...");
        ThreadManager.getInstance().execute(() -> {
            try {
                categoriaService.crearCategoria(categoriaACrear);
                Platform.runLater(() -> {
                    infoLabel.setText("Categoría creada.");
                    cargarDatosIniciales();
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleGenericError("Error", e));
            }
        });
    }

    @FXML
    private void handleCrearProducto() {
        if (categoriasData.isEmpty()) return;
        
        Dialog<ProductoDTO> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Producto");
        dialog.setHeaderText("Detalles del producto");
        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(); nombreField.setPromptText("Nombre");
        TextArea descripcionField = new TextArea(); descripcionField.setPromptText("Descripción");
        descripcionField.setPrefRowCount(2);
        TextField precioField = new TextField(); precioField.setPromptText("0.00");

        Label lblImagen = new Label("Imagen:");
        Button btnSeleccionarImagen = new Button("Elegir archivo...");
        Label lblRutaImagen = new Label("Ninguna");
        lblRutaImagen.setStyle("-fx-font-size: 10px;");

        final String[] selectedImageUrl = {null};

        btnSeleccionarImagen.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                lblRutaImagen.setText("Subiendo...");
                btnSeleccionarImagen.setDisable(true);
                ThreadManager.getInstance().execute(() -> {
                    try {
                        String uploadedUrl = subirImagenAlServidor(file);
                        Platform.runLater(() -> {
                            lblRutaImagen.setText("OK: " + file.getName());
                            selectedImageUrl[0] = uploadedUrl;
                            btnSeleccionarImagen.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            lblRutaImagen.setText("Error upload");
                            btnSeleccionarImagen.setDisable(false);
                        });
                    }
                });
            }
        });

        ComboBox<CategoriaDTO> comboCategoriaPadre = new ComboBox<>();
        comboCategoriaPadre.setItems(categoriasData.stream().filter(c -> c.getIdCategoriaPadre() == null).collect(Collectors.toCollection(FXCollections::observableArrayList)));
        ComboBox<CategoriaDTO> comboSubcategoria = new ComboBox<>();
        
        comboCategoriaPadre.valueProperty().addListener((o, old, catPadre) -> {
            comboSubcategoria.getItems().clear();
            if (catPadre != null) {
                comboSubcategoria.setItems(categoriasData.stream().filter(s -> catPadre.getIdCategoria().equals(s.getIdCategoriaPadre())).collect(Collectors.toCollection(FXCollections::observableArrayList)));
            }
        });

        grid.add(new Label("Nombre:"), 0, 0); grid.add(nombreField, 1, 0);
        grid.add(new Label("Precio:"), 0, 1); grid.add(precioField, 1, 1);
        grid.add(new Label("Cat:"), 0, 2); grid.add(comboCategoriaPadre, 1, 2);
        grid.add(new Label("Sub:"), 0, 3); grid.add(comboSubcategoria, 1, 3);
        grid.add(lblImagen, 0, 4); grid.add(new VBox(5, btnSeleccionarImagen, lblRutaImagen), 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                try {
                    ProductoDTO np = new ProductoDTO();
                    np.setNombre(nombreField.getText());
                    np.setPrecio(Double.parseDouble(precioField.getText()));
                    np.setImagenUrl(selectedImageUrl[0]);
                    if (comboSubcategoria.getValue() != null) np.setIdCategoria(comboSubcategoria.getValue().getIdCategoria());
                    else if (comboCategoriaPadre.getValue() != null) np.setIdCategoria(comboCategoriaPadre.getValue().getIdCategoria());
                    return np;
                } catch (Exception e) { return null; }
            } return null;
        });

        Optional<ProductoDTO> result = dialog.showAndWait();
        result.ifPresent(p -> {
            ThreadManager.getInstance().execute(() -> {
                try {
                    productoService.crearProducto(p);
                    Platform.runLater(() -> {
                         // --- IMPORTANTE: Limpiar caché tras CREAR ---
                         DataCacheService.getInstance().limpiarCache();
                         cargarDatosIniciales();
                    });
                } catch (Exception e) { e.printStackTrace(); }
            });
        });
    }

    private String subirImagenAlServidor(File file) throws Exception {
        String boundary = new BigInteger(256, new Random()).toString();
        String uploadEndpoint = AppConfig.getInstance().getApiBaseUrl() + "/api/media/upload";
        String token = SessionManager.getInstance().getToken();

        Map<String, Object> data = new HashMap<>();
        data.put("file", file.toPath());

        HttpRequest.BodyPublisher body = ofMimeMultipartData(data, boundary);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadEndpoint))
                .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                .header("Authorization", "Bearer " + token)
                .POST(body)
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) return response.body();
        else throw new IOException("Error upload: " + response.statusCode());
    }

    public static HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, Object> data, String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            byteArrays.add(separator);
            if (entry.getValue() instanceof Path) {
                var path = (Path) entry.getValue();
                String mimeType = Files.probeContentType(path);
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName() + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private void actualizarListaCompletaYTotal() {
        itemsCompletosData.clear();
        itemsCompletosData.addAll(itemsEnviadosData);
        itemsCompletosData.addAll(itemsNuevosData);
        actualizarTotalPedido();
    }

    private void actualizarTotalPedido() {
        double subtotal = 0.0;
        for (DetallePedidoMesaDTO detalle : itemsCompletosData) {
            detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
            subtotal += detalle.getSubtotal();
        }
        double igv = subtotal * 0.18;
        double total = subtotal + igv;
        subTotalPedidoLabel.setText(String.format("S/ %.2f", subtotal));
        igvPedidoLabel.setText(String.format("S/ %.2f", igv));
        totalPedidoLabel.setText(String.format("S/ %.2f", total));
     }

    private void actualizarEstadoCrearPedidoButton() {
        boolean deshabilitar = (mesaSeleccionada == null || itemsNuevosData.isEmpty());
        crearPedidoButton.setDisable(deshabilitar);
        if (pedidoActual == null) crearPedidoButton.setText("Enviar a Cocina");
        else crearPedidoButton.setText("Añadir al Pedido");
    }

    @FXML
    private void handleEnviarPedido() {
        if (mesaSeleccionada == null || itemsNuevosData.isEmpty()) return;
        
        infoLabel.setText("Enviando...");
        crearPedidoButton.setDisable(true);

        PedidoMesaDTO pedidoDTO = new PedidoMesaDTO();
        pedidoDTO.setIdMesa(mesaSeleccionada.getIdMesa());
        pedidoDTO.setEstado("ABIERTO");
        pedidoDTO.setDetalles(new ArrayList<>(itemsNuevosData));

        if (this.pedidoActual == null) {
            ThreadManager.getInstance().execute(() -> {
                try {
                    PedidoMesaDTO pedidoCreado = pedidoMesaService.crearPedido(pedidoDTO);
                    Platform.runLater(() -> {
                        infoLabel.setText("Pedido creado.");
                        resetearPanelPedido();
                        cargarDatosIniciales();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error crear", e);
                        crearPedidoButton.setDisable(false);
                    });
                }
            });
        } else {
            Long idPedidoAActualizar = this.pedidoActual.getIdPedidoMesa();
            ThreadManager.getInstance().execute(() -> {
                try {
                    PedidoMesaDTO pedidoActualizado = pedidoMesaService.actualizarPedido(idPedidoAActualizar, pedidoDTO);
                    Platform.runLater(() -> {
                        infoLabel.setText("Pedido actualizado.");
                        resetearPanelPedido();
                        cargarDatosIniciales();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error actualizar", e);
                        crearPedidoButton.setDisable(false);
                    });
                }
            });
        }
    }
    
    private void resetearPanelPedido() {
        handleVolverMesas();
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(contenido);
        alert.showAndWait();
    }
    
    private void mostrarAlertaError(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void mostrarCategorias(List<CategoriaDTO> categorias) {
        categoriasData.clear();
        categoriasListView.getItems().clear();
        if (categorias != null) {
            categoriasData.addAll(categorias);
            categoriasListView.setItems(FXCollections.observableArrayList(
                categorias.stream().filter(c -> c.getIdCategoriaPadre() == null).collect(Collectors.toList())
            ));
        }
     }

    private void handleAuthenticationError(HttpClientService.AuthenticationException e) {
        infoLabel.setText("Sesión inválida");
        mostrarAlerta("Error", "Sesión expirada.");
    }

    private void handleGenericError(String message, Exception e) {
        infoLabel.setText(message);
        System.err.println(message + ": " + e.getMessage());
    }
    
    @Override
    public void cleanup() {
        System.out.println("Limpiando Dashboard.");
    }
}