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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.services.WebSocketService;
import proyectopos.restauranteappfrontend.util.AppConfig; // Import necesario para la URL
import proyectopos.restauranteappfrontend.util.SessionManager;
import proyectopos.restauranteappfrontend.util.ThreadManager;

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
        infoLabel.setText("Cargando datos iniciales...");
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");

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
                        subCategoriasListView.setPlaceholder(new Label(subcategorias.isEmpty() ? "No hay subcategorías" : "Seleccione subcategoría"));

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

    private void renderizarProductos() {
        if (productosContainer == null) return;
        
        productosContainer.getChildren().clear();

        if (filteredProductos != null && !filteredProductos.isEmpty()) {
            for (ProductoDTO p : filteredProductos) {
                productosContainer.getChildren().add(crearTarjetaProducto(p));
            }
        } else {
            Label emptyLabel = new Label("No hay productos en esta categoría.");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-padding: 20;");
            productosContainer.getChildren().add(emptyLabel);
        }
    }

    private Node crearTarjetaProducto(ProductoDTO producto) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefSize(160, 210);
        card.getStyleClass().add("card"); 
        card.setStyle("-fx-cursor: hand; -fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(100);
        imageView.setFitWidth(140);
        imageView.setPreserveRatio(true);
        
        try {
            String urlImagen = null;
            try {
                 urlImagen = producto.getImagenUrl(); 
            } catch (Exception e) { /* Ignorar */ }

            String url = (urlImagen != null && !urlImagen.isBlank()) 
                         ? urlImagen 
                         : "https://via.placeholder.com/150?text=" + producto.getNombre().replace(" ", "+"); 
            
            imageView.setImage(new Image(url, true)); 
        } catch (Exception e) {
            System.err.println("No se pudo cargar imagen para: " + producto.getNombre());
        }

        Label lblNombre = new Label(producto.getNombre());
        lblNombre.setWrapText(true);
        lblNombre.setAlignment(Pos.CENTER);
        lblNombre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-alignment: center;");
        lblNombre.setMaxHeight(40); 

        Label lblPrecio = new Label(String.format("S/ %.2f", producto.getPrecio()));
        lblPrecio.setStyle("-fx-text-fill: #d97706; -fx-font-size: 13px; -fx-font-weight: bold;");

        card.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                handleSeleccionarProducto(producto);
                card.setOpacity(0.5);
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override public void run() { Platform.runLater(() -> card.setOpacity(1.0)); }
                }, 100);
            }
        });

        card.getChildren().addAll(imageView, lblNombre, lblPrecio);
        return card;
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
            System.err.println("Error procesando mensaje WS en Dashboard: " + e.getMessage());
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
        Text numeroMesaText = (Text) buttonContent.getChildren().get(0);
        Text estadoMesaText = (Text) buttonContent.getChildren().get(1);

        switch (tipoEvento) {
            case "PEDIDO_CREADO":
            case "PEDIDO_ACTUALIZADO":
                mesaDTO.setEstado("OCUPADA");
                mesaButton.getStyleClass().add("mesa-ocupada");
                estadoMesaText.setText("Ocupada");
                setButtonTextColor(numeroMesaText, estadoMesaText, "white");
                break;

            case "PEDIDO_LISTO":
                mesaDTO.setEstado("OCUPADA"); 
                mesaButton.getStyleClass().add("mesa-pagando");
                estadoMesaText.setText("¡LISTO!");
                setButtonTextColor(numeroMesaText, estadoMesaText, "#111827");
                break;

            case "PEDIDO_CERRADO":
            case "PEDIDO_CANCELADO":
                mesaDTO.setEstado("DISPONIBLE");
                mesaButton.getStyleClass().add("mesa-libre");
                estadoMesaText.setText("Libre");
                setButtonTextColor(numeroMesaText, estadoMesaText, "white");
                break;
                
            default:
                if ("DISPONIBLE".equals(mesaDTO.getEstado())) mesaButton.getStyleClass().add("mesa-libre");
                else if ("OCUPADA".equals(mesaDTO.getEstado())) mesaButton.getStyleClass().add("mesa-ocupada");
                break;
        }
        mesaButton.setUserData(mesaDTO);
    }

    private void setButtonTextColor(Text t1, Text t2, String color) {
        String style1 = t1.getStyle().replaceAll("-fx-fill: [^;]+;", "") + "-fx-fill: " + color + ";";
        String style2 = t2.getStyle().replaceAll("-fx-fill: [^;]+;", "") + "-fx-fill: " + color + ";";
        t1.setStyle(style1);
        t2.setStyle(style2);
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
                System.err.println("Error en la actualización de mesas vía WebSocket: " + e.getMessage());
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
        
        pedidoActualTableView.setPlaceholder(new Label("Añada productos (seleccione de la derecha)"));
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

        Button crearProductoBtn = new Button("Crear Producto");
        crearProductoBtn.getStyleClass().addAll("btn", "btn-info");
        crearProductoBtn.setOnAction(e -> handleCrearProducto());
        crearProductoBtn.setMaxWidth(Double.MAX_VALUE);

        Button gestionarCategoriasBtn = new Button("Gestionar Categorías");
        gestionarCategoriasBtn.getStyleClass().addAll("btn", "btn-secondary");
        gestionarCategoriasBtn.setOnAction(e -> handleGestionarCategorias());
        gestionarCategoriasBtn.setMaxWidth(Double.MAX_VALUE);

        adminButtonContainer = new HBox(10, crearProductoBtn, gestionarCategoriasBtn);
        adminButtonContainer.setAlignment(Pos.CENTER_LEFT);
        adminButtonContainer.setPadding(new Insets(0, 0, 5, 0));

        try {
            Node contenedorProductos = productosContainer;
            Node parent = contenedorProductos.getParent(); 
            while (parent != null && !(parent instanceof VBox)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof VBox) {
                VBox parentVBox = (VBox) parent;
                if (!parentVBox.getChildren().contains(adminButtonContainer)) {
                     parentVBox.getChildren().add(0, adminButtonContainer); 
                }
            }
        } catch (Exception e) {
            System.err.println("Error al intentar añadir botones de admin: " + e.getMessage());
        }
    }

    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos iniciales...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        setUIDisabledDuringLoad(true);

        ThreadManager.getInstance().execute(() -> {
            List<MesaDTO> mesas = null;
            List<CategoriaDTO> categorias = null;
            List<ProductoDTO> productos = null;
            List<PedidoMesaDTO> pedidosActivosInicial = null;
            Exception errorMesas = null, errorCategorias = null, errorProductos = null, errorPedidos = null;

            try { mesas = mesaService.getAllMesas(); } catch (Exception e) { errorMesas = e; System.err.println("Error al cargar mesas: " + e.getMessage()); }
            try { categorias = categoriaService.getAllCategorias(); } catch (Exception e) { errorCategorias = e; System.err.println("Error al cargar categorías: " + e.getMessage()); }
            try { productos = productoService.getAllProductos(); } catch (Exception e) { errorProductos = e; System.err.println("Error al cargar productos: " + e.getMessage()); }
            try {
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                pedidosActivosInicial = todosLosPedidos.stream()
                        .filter(p -> !"CERRADO".equalsIgnoreCase(p.getEstado()) && !"CANCELADO".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());
            } catch (Exception e) { errorPedidos = e; System.err.println("Error al cargar pedidos iniciales: " + e.getMessage()); }

            final List<MesaDTO> finalMesas = mesas;
            final List<CategoriaDTO> finalCategorias = categorias;
            final List<ProductoDTO> finalProductos = productos;
            final List<PedidoMesaDTO> finalPedidosActivos = pedidosActivosInicial;
            final Exception finalErrorMesas = errorMesas, finalErrorCategorias = errorCategorias, finalErrorProductos = errorProductos, finalErrorPedidos = errorPedidos;

            Platform.runLater(() -> {
                boolean huboErrorGeneral = false;
                HttpClientService.AuthenticationException authException = null;

                if (finalMesas != null) {
                     if (finalPedidosActivos != null) {
                        estadoPedidoCache.clear();
                        for (PedidoMesaDTO pedido : finalPedidosActivos) {
                            estadoPedidoCache.put(pedido.getIdMesa(), pedido.getEstado());
                        }
                    }
                    mostrarMesas(finalMesas);
                } else {
                    huboErrorGeneral = true;
                    mesasContainer.getChildren().clear();
                    mesasContainer.getChildren().add(new Label("Error al cargar mesas."));
                    if (finalErrorMesas instanceof HttpClientService.AuthenticationException) authException = (HttpClientService.AuthenticationException) finalErrorMesas;
                }
                if (finalCategorias != null) {
                    mostrarCategorias(finalCategorias);
                } else {
                    huboErrorGeneral = true;
                    categoriasListView.setPlaceholder(new Label("Error al cargar categorías."));
                    subCategoriasListView.setPlaceholder(new Label("Error"));
                     if (finalErrorCategorias instanceof HttpClientService.AuthenticationException) authException = (HttpClientService.AuthenticationException) finalErrorCategorias;
                }
                
                if (finalProductos != null) {
                    productosData.clear();
                    productosData.addAll(finalProductos);
                    filteredProductos = new FilteredList<>(productosData, p -> true);
                    renderizarProductos();
                } else {
                    huboErrorGeneral = true;
                    productosContainer.getChildren().add(new Label("Error al cargar productos."));
                    if (finalErrorProductos instanceof HttpClientService.AuthenticationException) authException = (HttpClientService.AuthenticationException) finalErrorProductos;
                }
                
                if (finalErrorPedidos instanceof HttpClientService.AuthenticationException) authException = (HttpClientService.AuthenticationException) finalErrorPedidos;

                if (huboErrorGeneral) {
                    infoLabel.setText("Error al cargar algunos datos iniciales.");
                    infoLabel.getStyleClass().setAll("lbl-danger");
                    if (authException != null) { handleAuthenticationError(authException); }
                } else {
                    infoLabel.setText("Datos cargados. Actualización automática iniciada.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                }
                
                configurarBotonesAdmin(); 
                
                setUIDisabledDuringLoad(false); 
            });
        });
    }

    private void setUIDisabledDuringLoad(boolean disabled) {
         if (mesasContainer != null) mesasContainer.setDisable(disabled);
         if (categoriasListView != null) categoriasListView.setDisable(disabled);
         if (subCategoriasListView != null) subCategoriasListView.setDisable(disabled);
         if (productosContainer != null) productosContainer.setDisable(disabled);
         if (gestionPedidoPane != null) gestionPedidoPane.setDisable(disabled);
    }

    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear();
        if (mesas != null && !mesas.isEmpty()) {
            for (MesaDTO mesa : mesas) {
                if ("BLOQUEADA".equals(mesa.getEstado())) {
                continue; 
            }
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
                        estadoMesaText.setText("Reservada");
                        mesaButton.setDisable(true);
                        break;
                    default:
                        mesaButton.getStyleClass().add("btn-secondary");
                        estadoMesaText.setText(mesa.getEstado());
                        mesaButton.setDisable(true);
                }
                if ("LISTO_PARA_ENTREGAR".equalsIgnoreCase(estadoPedidoParaMesa)) {
                    numeroMesaText.setStyle(numeroMesaText.getStyle() + "; -fx-fill: #111827;");
                    estadoMesaText.setStyle(estadoMesaText.getStyle() + "; -fx-fill: #111827;");
                } else {
                    numeroMesaText.setStyle(numeroMesaText.getStyle() + "; -fx-fill: white;");
                    estadoMesaText.setStyle(estadoMesaText.getStyle() + "; -fx-fill: white;");
                }
                buttonContent.getChildren().addAll(numeroMesaText, estadoMesaText);
                mesaButton.setGraphic(buttonContent);
                mesasContainer.getChildren().add(mesaButton);
            }
        } else {
            mesasContainer.getChildren().add(new Label(mesas == null ? "Error al cargar mesas." : "No se encontraron mesas."));
        }
    }

    private void handleSeleccionarMesa(MesaDTO mesa) {
        this.mesaSeleccionada = mesa;
        this.pedidoActual = null; 
        
        itemsEnviadosData.clear();
        itemsNuevosData.clear();
        itemsCompletosData.clear(); 
        
        actualizarListaCompletaYTotal(); 
        
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
            renderizarProductos(); 
        }

        gestionPedidoPane.setVisible(true);
        gestionPedidoPane.setManaged(true);

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.pedidoActual = null;
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Nueva Orden)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada. Añada productos al pedido.");
            infoLabel.getStyleClass().setAll("lbl-info");
            actualizarEstadoCrearPedidoButton();
            pedidoActualTableView.setPlaceholder(new Label("Añada productos (click en tarjeta)"));
        } else if ("OCUPADA".equals(mesa.getEstado())) {
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Orden Activa)");
            infoLabel.setText("Cargando pedido activo de Mesa " + mesa.getNumeroMesa() + "...");
            infoLabel.getStyleClass().setAll("lbl-warning");
            pedidoActualTableView.setPlaceholder(new Label("Cargando items..."));

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
                             infoLabel.setText("Mesa ocupada, pero sin pedido activo. Iniciando nueva orden.");
                        }
                        
                        pedidoActualTableView.setPlaceholder(new Label("Añada nuevos productos"));
                        actualizarListaCompletaYTotal(); 
                        actualizarEstadoCrearPedidoButton();
                        infoLabel.setText("Pedido de Mesa " + mesa.getNumeroMesa() + " cargado. Puede añadir más productos.");
                        infoLabel.getStyleClass().setAll("lbl-info");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                             this.pedidoActual = null;
                             mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Nueva Orden)");
                             infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada (Sin pedido activo). Añada productos.");
                             infoLabel.getStyleClass().setAll("lbl-info");
                             pedidoActualTableView.setPlaceholder(new Label("Añada productos"));
                             actualizarListaCompletaYTotal();
                             actualizarEstadoCrearPedidoButton();
                        } else {
                            handleGenericError("Error al cargar pedido activo", e);
                            mostrarAlertaError("Error", "No se pudo cargar el pedido activo para la mesa " + mesa.getNumeroMesa() + ".");
                            pedidoActualTableView.setPlaceholder(new Label("Error al cargar items."));
                            gestionPedidoPane.setVisible(false);
                            gestionPedidoPane.setManaged(false);
                            this.mesaSeleccionada = null;
                        }
                    });
                }
            });
        } else {
            this.mesaSeleccionada = null;
            this.pedidoActual = null;
            mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " está " + mesa.getEstado() + ".");
            infoLabel.getStyleClass().setAll("lbl-warning");
            gestionPedidoPane.setVisible(false);
            gestionPedidoPane.setManaged(false);
        }
     }

    private void handleSeleccionarProducto(ProductoDTO producto) {
        if (mesaSeleccionada == null) {
            mostrarAlerta("Acción no permitida", "Seleccione una mesa LIBRE u OCUPADA para añadir productos.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Añadir Producto");
        dialog.setHeaderText("Añadir '" + producto.getNombre() + "' al pedido");
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

                } else {
                    mostrarAlerta("Cantidad inválida", "La cantidad debe ser un número positivo.");
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Entrada inválida", "Por favor, ingrese un número válido para la cantidad.");
            }
        });
    }


    @FXML
    private void handleGestionarCategorias() {
        if (categoriasData.isEmpty()) {
            mostrarAlerta("Error", "Las categorías aún no se han cargado. Espere e intente de nuevo.");
            return;
        }
        Dialog<CategoriaDTO> dialog = new Dialog<>();
        dialog.setTitle("Gestionar Categorías");
        dialog.setHeaderText("Crear nueva categoría o subcategoría.");
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
        categoriaPadreComboBox.setPromptText("Opcional: Categoría padre");
        grid.add(new Label("Nombre:"), 0, 0); grid.add(nombreField, 1, 0);
        grid.add(new Label("Categoría Padre:"), 0, 1); grid.add(categoriaPadreComboBox, 1, 1);
        dialog.getDialogPane().setContent(grid);
        Node crearButton = dialog.getDialogPane().lookupButton(crearButtonType);
        crearButton.setDisable(true);
        nombreField.textProperty().addListener((o, ov, nv) -> crearButton.setDisable(nv.trim().isEmpty()));
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
        infoLabel.setText("Creando categoría '" + categoriaACrear.getNombre() + "'...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        ThreadManager.getInstance().execute(() -> {
            try {
                CategoriaDTO categoriaCreada = categoriaService.crearCategoria(categoriaACrear);
                Platform.runLater(() -> {
                    infoLabel.setText("Categoría '" + categoriaCreada.getNombre() + "' creada.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                    cargarDatosIniciales(); 
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleGenericError("Error al crear la categoría", e));
            }
        });
    }

    @FXML
    private void handleCrearProducto() {
        if (categoriasData.isEmpty()) {
            mostrarAlerta("Error", "Las categorías aún no se han cargado. Espere e intente de nuevo.");
            return;
        }
        Dialog<ProductoDTO> dialog = new Dialog<>();
        dialog.setTitle("Crear Nuevo Producto");
        dialog.setHeaderText("Ingrese los detalles del nuevo producto.");
        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField nombreField = new TextField(); nombreField.setPromptText("Nombre del producto");
        TextArea descripcionField = new TextArea(); descripcionField.setPromptText("Descripción (opcional)");
        descripcionField.setWrapText(true); descripcionField.setPrefRowCount(3);
        TextField precioField = new TextField(); precioField.setPromptText("Precio (ej. 15.50)");
        
        Label lblImagen = new Label("Imagen:");
        Button btnSeleccionarImagen = new Button("Seleccionar archivo...");
        btnSeleccionarImagen.getStyleClass().add("btn-secondary");
        ImageView imgPreview = new ImageView();
        imgPreview.setFitWidth(100);
        imgPreview.setFitHeight(100);
        imgPreview.setPreserveRatio(true);
        Label lblRutaImagen = new Label("Sin imagen");
        lblRutaImagen.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
        
        final String[] selectedImageUrl = {null}; 
        
        btnSeleccionarImagen.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Imagen del Producto");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = fileChooser.showOpenDialog(btnSeleccionarImagen.getScene().getWindow());
            if (file != null) {
                lblRutaImagen.setText("Subiendo: " + file.getName() + "...");
                btnSeleccionarImagen.setDisable(true);
                imgPreview.setImage(null); // Limpiar mientras carga

                ThreadManager.getInstance().execute(() -> {
                    try {
                        // 1. Subir imagen y obtener URL remota
                        String uploadedUrl = subirImagenAlServidor(file);
                        
                        Platform.runLater(() -> {
                            lblRutaImagen.setText(file.getName() + " (Subida)");
                            lblRutaImagen.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                            // Mostrar la imagen localmente para rapidez, o usar la remota
                            imgPreview.setImage(new Image(file.toURI().toString())); 
                            
                            // 2. Guardar la URL remota en la variable que usará el producto
                            selectedImageUrl[0] = uploadedUrl;
                            btnSeleccionarImagen.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            lblRutaImagen.setText("Error al subir.");
                            lblRutaImagen.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
                            mostrarAlertaError("Error de Subida", "No se pudo subir la imagen: " + e.getMessage());
                            btnSeleccionarImagen.setDisable(false);
                            e.printStackTrace();
                        });
                    }
                });
            }
        });

        ComboBox<CategoriaDTO> comboCategoriaPadre = new ComboBox<>();
        ObservableList<CategoriaDTO> categoriasPadre = categoriasData.stream()
                .filter(c -> c.getIdCategoriaPadre() == null)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        comboCategoriaPadre.setItems(categoriasPadre);
        comboCategoriaPadre.setPromptText("Seleccione categoría principal");
        ComboBox<CategoriaDTO> comboSubcategoria = new ComboBox<>();
        comboSubcategoria.setPromptText("Seleccione subcategoría");
        comboSubcategoria.setDisable(true);
        comboCategoriaPadre.valueProperty().addListener((observable, oldValue, catPadre) -> {
            comboSubcategoria.getItems().clear(); comboSubcategoria.setValue(null);
            if (catPadre != null) {
                ObservableList<CategoriaDTO> subcategorias = categoriasData.stream()
                        .filter(sub -> catPadre.getIdCategoria().equals(sub.getIdCategoriaPadre()))
                        .collect(Collectors.toCollection(FXCollections::observableArrayList));
                if (!subcategorias.isEmpty()) {
                    comboSubcategoria.setItems(subcategorias); comboSubcategoria.setDisable(false);
                } else { comboSubcategoria.setPromptText("No hay subcategorías"); comboSubcategoria.setDisable(true); }
            } else { comboSubcategoria.setDisable(true); }
        });
        
        grid.add(new Label("Nombre:"), 0, 0); grid.add(nombreField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1); grid.add(descripcionField, 1, 1);
        grid.add(new Label("Precio:"), 0, 2); grid.add(precioField, 1, 2);
        
        grid.add(lblImagen, 0, 3); 
        VBox imagenBox = new VBox(5, btnSeleccionarImagen, lblRutaImagen, imgPreview);
        grid.add(imagenBox, 1, 3);
        
        grid.add(new Label("Categoría:"), 0, 4); grid.add(comboCategoriaPadre, 1, 4);
        grid.add(new Label("Subcategoría:"), 0, 5); grid.add(comboSubcategoria, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        Node crearButton = dialog.getDialogPane().lookupButton(crearButtonType);
        crearButton.setDisable(true);
        Runnable validador = () -> {
            boolean invalido = nombreField.getText().trim().isEmpty() || precioField.getText().trim().isEmpty() || comboSubcategoria.getValue() == null;
            crearButton.setDisable(invalido);
        };
        nombreField.textProperty().addListener((o, ov, nv) -> validador.run());
        precioField.textProperty().addListener((o, ov, nv) -> validador.run());
        comboSubcategoria.valueProperty().addListener((o, ov, nv) -> validador.run());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                try {
                    ProductoDTO np = new ProductoDTO(); 
                    np.setNombre(nombreField.getText().trim());
                    np.setDescripcion(descripcionField.getText().trim());
                    double precio = Double.parseDouble(precioField.getText().trim());
                    if (precio <= 0) throw new NumberFormatException("Precio debe ser positivo");
                    np.setPrecio(precio); 
                    
                    if (selectedImageUrl[0] != null) {
                        np.setImagenUrl(selectedImageUrl[0]); // Guardamos la URL del servidor
                    }
                    np.setIdCategoria(comboSubcategoria.getValue().getIdCategoria());
                    return np;
                } catch (NumberFormatException e) { mostrarAlerta("Datos Inválidos", "El precio debe ser un número positivo."); return null; }
            } return null;
        });
        
        Optional<ProductoDTO> result = dialog.showAndWait();
        result.ifPresent(productoACrear -> {
            if (productoACrear == null) return;
            infoLabel.setText("Creando producto '" + productoACrear.getNombre() + "'...");
            infoLabel.getStyleClass().setAll("lbl-warning");
            ThreadManager.getInstance().execute(() -> {
                try {
                    ProductoDTO productoCreado = productoService.crearProducto(productoACrear);
                    Platform.runLater(() -> {
                        infoLabel.setText("Producto '" + productoCreado.getNombre() + "' creado.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        cargarDatosIniciales(); // Recargar para ver el nuevo producto
                    });
                } catch (Exception e) { Platform.runLater(() -> handleGenericError("Error al crear el producto", e)); }
            });
        });
    }
    
    /**
     * Método privado para subir la imagen al backend usando Multipart.
     */
    private String subirImagenAlServidor(File file) throws Exception {
        String boundary = new BigInteger(256, new Random()).toString();
        String baseUrl = AppConfig.getInstance().getApiBaseUrl();
        String uploadEndpoint = baseUrl + "/api/media/upload";
        String token = SessionManager.getInstance().getToken();

        // Construir el cuerpo Multipart manualmente
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

        if (response.statusCode() == 200) {
            return response.body(); // El backend devuelve la URL como string
        } else {
            throw new IOException("Error del servidor (" + response.statusCode() + "): " + response.body());
        }
    }

    // Helper para construir multipart (Java 11 standard no lo tiene nativo simple)
    public static HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, Object> data, String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            byteArrays.add(separator);

            if (entry.getValue() instanceof Path) {
                var path = (Path) entry.getValue();
                String mimeType = Files.probeContentType(path);
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
                        + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
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
        
        if (pedidoActual == null) {
            crearPedidoButton.setText("Enviar Pedido a Cocina");
        } else {
            crearPedidoButton.setText("Añadir Items al Pedido"); 
        }
    }


    @FXML
    private void handleEnviarPedido() {
        if (mesaSeleccionada == null || itemsNuevosData.isEmpty()) {
            mostrarAlerta("Pedido incompleto", "Debe seleccionar una mesa y añadir al menos un producto nuevo.");
            return;
        }
        infoLabel.setText("Enviando pedido...");
        infoLabel.getStyleClass().setAll("lbl-warning");
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
                        infoLabel.setText("Pedido #" + pedidoCreado.getIdPedidoMesa() + " creado para Mesa " + mesaSeleccionada.getNumeroMesa());
                        infoLabel.getStyleClass().setAll("lbl-success");
                        resetearPanelPedido();
                        cargarDatosIniciales(); // Recarga todo
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al crear el pedido", e);
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
                        infoLabel.setText("Items añadidos al Pedido #" + pedidoActualizado.getIdPedidoMesa() + " (Mesa " + mesaSeleccionada.getNumeroMesa() + ").");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        resetearPanelPedido();
                        cargarDatosIniciales(); // Recarga todo
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al actualizar el pedido", e);
                        crearPedidoButton.setDisable(false);
                    });
                }
            });
        }
    }
    private void resetearPanelPedido() {
        this.mesaSeleccionada = null;
        this.pedidoActual = null;
        
        itemsCompletosData.clear();
        itemsEnviadosData.clear();
        itemsNuevosData.clear();
        
        actualizarTotalPedido(); 
        actualizarEstadoCrearPedidoButton();
        
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
        
        // Limpiar vista de productos al salir
        if (productosContainer != null) productosContainer.getChildren().clear();
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
        if (categorias != null && !categorias.isEmpty()) {
            categoriasData.addAll(categorias);
            List<CategoriaDTO> categoriasPrincipales = categorias.stream()
                    .filter(c -> c.getIdCategoriaPadre() == null)
                    .collect(Collectors.toList());
            categoriasListView.setItems(FXCollections.observableArrayList(categoriasPrincipales));
            categoriasListView.setPlaceholder(null);
        } else {
            categoriasListView.setPlaceholder(new Label(categorias == null ? "Error al cargar categorías." : "No se encontraron categorías."));
        }
     }
     
    private void mostrarProductos(List<ProductoDTO> productos) {
        // Método antiguo para Tabla, ahora delega al renderizador de tarjetas
        productosData.clear();
        if (productos != null && !productos.isEmpty()) {
            productosData.addAll(productos);
        }
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
        }
        renderizarProductos();
    }


    private void handleAuthenticationError(HttpClientService.AuthenticationException e) {
        infoLabel.setText(e.getMessage());
        infoLabel.getStyleClass().setAll("lbl-danger");
        System.err.println("Error de autenticación, se debería redirigir al login.");
        categoriasListView.setPlaceholder(new Label("Error de autenticación."));
        if (productosContainer != null) productosContainer.getChildren().add(new Label("Error de autenticación."));
        mesasContainer.getChildren().clear();
        mesasContainer.getChildren().add(new Label("Error de autenticación."));
        mostrarAlerta("Sesión Expirada", "Su sesión ha expirado o no es válida. Por favor, vuelva a iniciar sesión.");
    }

    private void handleGenericError(String message, Exception e) {
        String errorMessage = (message != null && !message.isBlank()) ? message : "Error inesperado.";
        if (e != null) { errorMessage += ": " + e.getMessage(); e.printStackTrace(); }
        infoLabel.setText(errorMessage);
        infoLabel.getStyleClass().setAll("lbl-danger");
        if (categoriasListView.getItems().isEmpty()) categoriasListView.setPlaceholder(new Label("Error al cargar."));
        if (productosContainer != null && productosContainer.getChildren().isEmpty()) productosContainer.getChildren().add(new Label("Error al cargar."));
         if (mesasContainer.getChildren().size() <=1 && mesasContainer.getChildren().get(0) instanceof Label) mesasContainer.getChildren().set(0, new Label("Error al cargar."));
        mostrarAlertaError("Error", errorMessage);
    }
      @Override
        public void cleanup() {
        System.out.println("Limpiando DashboardController.");
    }
}