package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.controllers.helpers.DashboardWebSocketHandler;
import proyectopos.restauranteappfrontend.controllers.listeners.DashboardUpdateListener;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.DataCacheService;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.services.WebSocketService;
import proyectopos.restauranteappfrontend.util.SessionManager; 
import proyectopos.restauranteappfrontend.util.ThreadManager; 

public class DashboardController implements CleanableController, DashboardUpdateListener {

    @FXML private Label infoLabel;
    @FXML private TilePane mesasContainer;
    @FXML private VBox gestionPedidoPane;
    @FXML private ListView<CategoriaDTO> categoriasListView;
    @FXML private ListView<CategoriaDTO> subCategoriasListView;
    @FXML private TilePane productosContainer;
    @FXML private Label mesaSeleccionadaLabel;

    @FXML private VBox contenedorProductos; 

    @FXML private OrderPanelController orderPanelController;

    private HBox adminButtonContainer = null;

    // Servicios
    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();

    // --- Delegados (Helpers) ---
    private CatalogManagementController catalogController;
    private DashboardWebSocketHandler wsHandler; // Se encarga de la lógica sucia de WS

    // Estado UI
    private MesaDTO mesaSeleccionada = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> categoriasData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> subCategoriasData = FXCollections.observableArrayList();
    private FilteredList<ProductoDTO> filteredProductos;
    
    // Cache de estados y Mapa de UI
    private final Map<Long, String> estadoPedidoCache = new HashMap<>();
    private final Map<Long, MesaTileController> mapaMesasUI = new HashMap<>();

    @FXML
    public void initialize() {
        infoLabel.setText("Cargando sistema...");
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");

        // 1. Inicializar el Gestor de Catálogo (Administración)
        this.catalogController = new CatalogManagementController(() -> 
            ThreadManager.getInstance().execute(this::cargarCatalogo)
        );

        // 2. Inicializar el Gestor de WebSockets (Tiempo Real)
        // Le pasamos 'this' (el listener) y el caché de estados para que tome decisiones
        this.wsHandler = new DashboardWebSocketHandler(this, this.estadoPedidoCache);

        // Configuración inicial de UI
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);
        categoriasListView.setPlaceholder(new Label("Cargando categorías..."));
        subCategoriasListView.setPlaceholder(new Label("Seleccione categoría"));
        subCategoriasListView.setItems(subCategoriasData);

        if (productosContainer != null) {
            productosContainer.getChildren().clear();
            productosContainer.getChildren().add(new Label("Cargando productos..."));
        }
        mesasContainer.getChildren().clear();
        mesasContainer.getChildren().add(new Label("Cargando mesas..."));

        // Carga de Datos
        configurarContenedorMesas();
        cargarDatosIniciales();
        setupCategoryListeners();

        // 3. Suscripción a WebSockets delegada
        // El controlador solo recibe el String y se lo pasa al Handler
        WebSocketService.getInstance().subscribe("/topic/pedidos", (jsonMessage) -> {
            Platform.runLater(() -> wsHandler.procesarMensaje(jsonMessage, this.mesaSeleccionada));
        });
        
        if (orderPanelController != null) {
            orderPanelController.setOnPedidoEnviado(() -> {
                infoLabel.setText("Pedido enviado a cocina.");
                infoLabel.getStyleClass().setAll("lbl-success");
            });
        } else {
            System.err.println("Error: orderPanelController no fue inyectado.");
        }
    }

    @FXML
    private void handleVolverMesas() {
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);
        this.mesaSeleccionada = null;
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
        
        if (orderPanelController != null) {
            orderPanelController.setMesa(null, null);
        }
        
        // Recargar estado visual
        ThreadManager.getInstance().execute(this::cargarEstadoMesas);
        infoLabel.setText("Seleccione una mesa.");
    }

    // --- IMPLEMENTACIÓN DE DashboardUpdateListener (Eventos de Negocio) ---
    @Override
    public void onSystemRefreshRequested() {
        // El Handler solicita un refresco total 
        ThreadManager.getInstance().execute(this::cargarEstadoMesas);
    }

    @Override
    public void onMesaStatusChanged(Long idMesa, String nuevoEstadoBase, String nuevoEstadoPedido) {
        // Actualización visual puntual O(1)
        MesaTileController controller = mapaMesasUI.get(idMesa);
        if (controller != null) {
            controller.actualizarEstadoVisual(nuevoEstadoBase, nuevoEstadoPedido);
        }
    }

    @Override
    public void onPedidoActiveUpdated(PedidoMesaDTO pedido) {
        // Si estamos viendo ese pedido, actualizamos el panel lateral
        if (orderPanelController != null) {
            orderPanelController.setMesa(mesaSeleccionada, pedido); 
        }
        infoLabel.setText("Pedido actualizado remotamente.");
    }

    @Override
    public void onPedidoActiveClosed(String mensaje) {
        // Si nos cierran el pedido en la cara (Caja), volvemos al inicio
        handleVolverMesas();
        infoLabel.setText(mensaje);
        mostrarAlertaInfo("Aviso", mensaje);
    }

    // --- Lógica de Selección de Mesa (UI) ---

    private void handleSeleccionarMesa(MesaDTO mesa) {
        this.mesaSeleccionada = mesa;
        
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
            renderizarProductos();
        }

        gestionPedidoPane.setVisible(true);
        gestionPedidoPane.setManaged(true);
        mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa());

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            infoLabel.setText("Nueva orden para Mesa " + mesa.getNumeroMesa());
            if (orderPanelController != null) {
                orderPanelController.setMesa(mesa, null);
            }
        } else if ("OCUPADA".equals(mesa.getEstado())) {
            infoLabel.setText("Cargando pedido...");
            ThreadManager.getInstance().execute(() -> {
                try {
                    PedidoMesaDTO pedidoCargado = pedidoMesaService.getPedidoActivoPorMesa(mesa.getIdMesa());
                    Platform.runLater(() -> {
                        if (orderPanelController != null) {
                            orderPanelController.setMesa(mesa, pedidoCargado);
                        }
                        infoLabel.setText("Pedido cargado.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                             if (orderPanelController != null) orderPanelController.setMesa(mesa, null);
                        } else {
                            mostrarAlertaError("Error", "No se pudo cargar el pedido.");
                            handleVolverMesas();
                        }
                    });
                }
            });
        } else {
            handleVolverMesas();
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
                if (cantidad > 0 && orderPanelController != null) {
                    orderPanelController.agregarProducto(producto, cantidad);
                } 
            } catch (NumberFormatException e) { /* Ignorar */ }
        });
    }

    // --- Carga de Datos ---

    private void cargarDatosIniciales() {
        infoLabel.setText("Iniciando sistema...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        setUIDisabledDuringLoad(true);

        ThreadManager.getInstance().execute(() -> {
            cargarCatalogo(); 
            cargarEstadoMesas();
            
            Platform.runLater(() -> {
                infoLabel.setText("Sistema listo.");
                infoLabel.getStyleClass().setAll("lbl-success");
                configurarBotonesAdmin();
                setUIDisabledDuringLoad(false);
            });
        });
    }

    private void cargarCatalogo() {
        try {
            List<CategoriaDTO> cats = categoriaService.getAllCategorias();
            List<ProductoDTO> prods;
            if (DataCacheService.getInstance().getProductos() != null) {
                prods = DataCacheService.getInstance().getProductos();
            } else {
                prods = productoService.getAllProductos();
                DataCacheService.getInstance().setProductos(prods);
            }

            Platform.runLater(() -> {
                mostrarCategorias(cats);
                productosData.setAll(prods);
                filteredProductos = new FilteredList<>(productosData, p -> true);
                renderizarProductos();
            });
        } catch (Exception e) {
            Platform.runLater(() -> handleGenericError("Error cargando catálogo", e));
        }
    }

    private void cargarEstadoMesas() {
        try {
            List<MesaDTO> mesas = mesaService.getAllMesas();
            List<PedidoMesaDTO> pedidos = pedidoMesaService.getAllPedidos(); 
            
            // Actualizamos el caché compartido con el WSHandler
            estadoPedidoCache.clear();
            if (pedidos != null) {
                for (PedidoMesaDTO p : pedidos) {
                    if (!"CERRADO".equals(p.getEstado()) && !"CANCELADO".equals(p.getEstado())) {
                        estadoPedidoCache.put(p.getIdMesa(), p.getEstado());
                    }
                }
            }

            Platform.runLater(() -> mostrarMesas(mesas));
        } catch (HttpClientService.AuthenticationException e) {
            Platform.runLater(() -> handleAuthenticationError(e));
        } catch (Exception e) {
            Platform.runLater(() -> handleGenericError("Error sincronizando mesas", e));
        }
    }

    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear();
        mapaMesasUI.clear();

        if (mesas != null && !mesas.isEmpty()) {
            for (MesaDTO mesa : mesas) {
                if ("BLOQUEADA".equals(mesa.getEstado())) continue;
                
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/mesa-tile.fxml"));
                    Button mesaNode = loader.load();
                    MesaTileController controller = loader.getController();
                    
                    String estadoPedido = estadoPedidoCache.get(mesa.getIdMesa());
                    controller.setMesaData(mesa, this::handleSeleccionarMesa);
                    controller.actualizarEstadoVisual(mesa.getEstado(), estadoPedido);
                    mesaNode.setUserData(controller); 
                    
                    mapaMesasUI.put(mesa.getIdMesa(), controller);
                    mesasContainer.getChildren().add(mesaNode);
                } catch (IOException e) { e.printStackTrace(); }
            }
        } else {
            mesasContainer.getChildren().add(new Label("Sin mesas."));
        }
    }

    // --- Métodos de UI Auxiliares ---

    private void setupCategoryListeners() {
        categoriasListView.getSelectionModel().selectedItemProperty().addListener((obs, old, cat) -> {
            subCategoriasData.clear();
            subCategoriasListView.getSelectionModel().clearSelection();
            if (cat != null) {
                infoLabel.setText("Categoría: " + cat.getNombre());
                List<CategoriaDTO> subs = categoriasData.stream()
                        .filter(c -> cat.getIdCategoria().equals(c.getIdCategoriaPadre()))
                        .collect(Collectors.toList());
                subCategoriasData.addAll(subs);
                
                if (filteredProductos != null) {
                    Set<Long> ids = subs.stream().map(CategoriaDTO::getIdCategoria).collect(Collectors.toSet());
                    filteredProductos.setPredicate(p -> ids.isEmpty() ? 
                        cat.getIdCategoria().equals(p.getIdCategoria()) : ids.contains(p.getIdCategoria()));
                }
            } else {
                if (filteredProductos != null) filteredProductos.setPredicate(p -> true);
                infoLabel.setText("Seleccione una categoría");
            }
            renderizarProductos();
        });

        subCategoriasListView.getSelectionModel().selectedItemProperty().addListener((obs, old, subCat) -> {
            if (filteredProductos == null) return;
            if (subCat != null) {
                infoLabel.setText("Subcategoría: " + subCat.getNombre());
                filteredProductos.setPredicate(p -> p.getIdCategoria().equals(subCat.getIdCategoria()));
            } else {
                // Lógica para volver a filtrar por padre si se deselecciona hijo
                CategoriaDTO parent = categoriasListView.getSelectionModel().getSelectedItem();
                if (parent != null) {
                     List<CategoriaDTO> subs = categoriasData.stream()
                        .filter(c -> parent.getIdCategoria().equals(c.getIdCategoriaPadre()))
                        .collect(Collectors.toList());
                     Set<Long> ids = subs.stream().map(CategoriaDTO::getIdCategoria).collect(Collectors.toSet());
                     filteredProductos.setPredicate(p -> ids.isEmpty() ? 
                        parent.getIdCategoria().equals(p.getIdCategoria()) : ids.contains(p.getIdCategoria()));
                } else {
                    filteredProductos.setPredicate(p -> true);
                }
            }
            renderizarProductos();
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
            emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-padding: 20;");
            productosContainer.getChildren().add(emptyLabel);
        }
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

    private Node crearTarjetaProducto(ProductoDTO producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/producto-card.fxml"));
            Node cardNode = loader.load();
            ProductoCardController controller = loader.getController();
            // Delegación limpia al CatalogController
            controller.setData(producto, 
                this::handleSeleccionarProducto, 
                p -> catalogController.editarProducto(p), 
                p -> catalogController.eliminarProducto(p)
            );
            return cardNode;
        } catch (IOException e) { return new Label("Error"); }
    }

    // --- Métodos FXML Delegados ---

    @FXML private void handleCrearProducto() { catalogController.crearProducto(); }
    @FXML private void handleGestionarCategorias() { catalogController.gestionarCategorias(); }

    private void configurarBotonesAdmin() {
        String userRole = SessionManager.getInstance().getRole();
        
        // Limpiar botones anteriores si existen
        if (adminButtonContainer != null && contenedorProductos != null) {
             contenedorProductos.getChildren().remove(adminButtonContainer);
        }
        adminButtonContainer = null;

        if ("ROLE_ADMIN".equals(userRole)) {
            Button crearBtn = new Button("+ Nuevo Producto");
            crearBtn.getStyleClass().addAll("btn-sm", "btn-info");
            crearBtn.setOnAction(e -> handleCrearProducto());

            Button catBtn = new Button("Categorías");
            catBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
            catBtn.setOnAction(e -> handleGestionarCategorias());

            adminButtonContainer = new HBox(10, crearBtn, catBtn);
            adminButtonContainer.setAlignment(Pos.CENTER_LEFT);
            adminButtonContainer.setPadding(new Insets(0, 0, 10, 0));

            // Lógica corregida: Usamos directamente el contenedor inyectado
            if (contenedorProductos != null) {
                if (!contenedorProductos.getChildren().contains(adminButtonContainer)) {
                     // Añadimos los botones después del Label "Menú" (índice 1)
                     contenedorProductos.getChildren().add(1, adminButtonContainer); 
                }
            }
        }
    }

    // --- Utilidades ---

    private void configurarContenedorMesas() { }

    private void setUIDisabledDuringLoad(boolean disabled) {
        if (mesasContainer != null) mesasContainer.setDisable(disabled);
        if (categoriasListView != null) categoriasListView.setDisable(disabled);
        if (productosContainer != null) productosContainer.setDisable(disabled);
    }

    private void handleAuthenticationError(HttpClientService.AuthenticationException e) {
        infoLabel.setText("Sesión inválida");
        mostrarAlertaError("Error de Sesión", "Su sesión ha expirado. Por favor inicie sesión nuevamente.");
    }

    private void handleGenericError(String message, Exception e) {
        infoLabel.setText(message);
        System.err.println(message + ": " + e.getMessage());
    }

    private void mostrarAlertaError(String titulo, String contenido) {
        new Alert(Alert.AlertType.ERROR, contenido).showAndWait();
    }
    
    private void mostrarAlertaInfo(String titulo, String contenido) {
        new Alert(Alert.AlertType.INFORMATION, contenido).showAndWait();
    }

    @Override
    public void cleanup() {
        System.out.println("Limpiando Dashboard.");
    }
}