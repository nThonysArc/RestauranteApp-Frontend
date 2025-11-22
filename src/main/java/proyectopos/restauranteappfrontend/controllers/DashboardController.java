package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.model.dto.WebSocketMessageDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.DataCacheService;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.services.WebSocketService;
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

    // --- CONTROLADOR HIJO INYECTADO (fx:id="orderPanel" + "Controller") ---
    @FXML private OrderPanelController orderPanelController;

    private HBox adminButtonContainer = null;

    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    private final Gson gson = new Gson();

    private MesaDTO mesaSeleccionada = null;
    
    // Datos
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
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
        subCategoriasListView.setItems(subCategoriasData);

        if (productosContainer != null) {
            productosContainer.getChildren().clear();
            productosContainer.getChildren().add(new Label("Cargando productos..."));
        }

        mesasContainer.getChildren().clear();
        mesasContainer.getChildren().add(new Label("Cargando mesas..."));

        configurarContenedorMesas();
        cargarDatosIniciales();

        // Configurar Listeners de Categorías
        setupCategoryListeners();

        // Configurar WebSockets
        WebSocketService.getInstance().subscribe("/topic/pedidos", (jsonMessage) -> {
            Platform.runLater(() -> procesarMensajeWebSocket(jsonMessage));
        });
        
        // Configurar Callback del Panel de Orden
        if (orderPanelController != null) {
            orderPanelController.setOnPedidoEnviado(() -> {
                infoLabel.setText("Pedido enviado a cocina.");
                infoLabel.getStyleClass().setAll("lbl-success");
                // Opcional: Volver a mesas automáticamente
                // handleVolverMesas();
            });
        } else {
            System.err.println("Error: orderPanelController no fue inyectado. Verifique fx:id en FXML.");
        }
    }

    @FXML
    private void handleVolverMesas() {
        // Ocultar panel de pedidos
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);
        
        // Limpiar selección
        this.mesaSeleccionada = null;
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
        
        // Resetear el panel hijo para que no muestre datos viejos
        if (orderPanelController != null) {
            orderPanelController.setMesa(null, null);
        }
        
        // Refrescar mesas (para ver estados actualizados)
        cargarSoloMesasAsync();
        
        infoLabel.setText("Seleccione una mesa.");
    }

    // --- Lógica de Selección de Mesa ---
    private void handleSeleccionarMesa(MesaDTO mesa) {
        this.mesaSeleccionada = mesa;
        
        // Resetear filtros de productos
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
            renderizarProductos();
        }

        // Mostrar la vista de pedido
        gestionPedidoPane.setVisible(true);
        gestionPedidoPane.setManaged(true);
        mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa());

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            infoLabel.setText("Nueva orden para Mesa " + mesa.getNumeroMesa());
            // Inicializar panel vacío para mesa nueva en el sub-controlador
            if (orderPanelController != null) {
                orderPanelController.setMesa(mesa, null);
            }

        } else if ("OCUPADA".equals(mesa.getEstado())) {
            infoLabel.setText("Cargando pedido...");
            
            ThreadManager.getInstance().execute(() -> {
                try {
                    PedidoMesaDTO pedidoCargado = pedidoMesaService.getPedidoActivoPorMesa(mesa.getIdMesa());
                    Platform.runLater(() -> {
                        // Pasar el pedido cargado al sub-controlador
                        if (orderPanelController != null) {
                            orderPanelController.setMesa(mesa, pedidoCargado);
                        }
                        infoLabel.setText("Pedido cargado.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                             // Mesa ocupada sin pedido activo (inconsistencia o recién cerrada)
                             if (orderPanelController != null) orderPanelController.setMesa(mesa, null);
                        } else {
                            mostrarAlertaError("Error", "No se pudo cargar el pedido.");
                            handleVolverMesas();
                        }
                    });
                }
            });
        } else {
            handleVolverMesas(); // Si está reservada u otro estado
        }
    }

    // --- Lógica de Selección de Producto (Delegada) ---
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
                    // DELEGACIÓN: El sub-controlador maneja la lógica de agregar
                    orderPanelController.agregarProducto(producto, cantidad);
                } 
            } catch (NumberFormatException e) { /* Ignorar */ }
        });
    }

    // --- Configuración de Listeners de Categorías ---
    private void setupCategoryListeners() {
        // Listener Categorías Principales
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
                    if (filteredProductos != null) filteredProductos.setPredicate(p -> true);
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
                    // Volver a la categoría padre si se deselecciona
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

    private Node crearTarjetaProducto(ProductoDTO producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/producto-card.fxml"));
            Node cardNode = loader.load();
            ProductoCardController controller = loader.getController();
            controller.setData(producto, 
                this::handleSeleccionarProducto, 
                this::handleEditarProducto, 
                this::handleEliminarProducto
            );
            return cardNode;
        } catch (IOException e) {
            System.err.println("Error al cargar tarjeta de producto: " + e.getMessage());
            return new Label("Error: " + producto.getNombre());
        }
    }

    // --- WebSockets ---

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

                // Si la mesa seleccionada es la que recibió la actualización
                if (mesaSeleccionada != null && mesaSeleccionada.getIdMesa().equals(pedido.getIdMesa())) {
                    if ("PEDIDO_CERRADO".equals(msg.getType()) || "PEDIDO_CANCELADO".equals(msg.getType())) {
                        handleVolverMesas();
                        infoLabel.setText("El pedido ha sido cerrado remotamente.");
                        mostrarAlertaInfo("Aviso", "El pedido de esta mesa ha sido cerrado.");
                    } else {
                         // Actualizar el panel lateral con los nuevos datos
                         if (orderPanelController != null) {
                             // Al setear la mesa de nuevo con el pedido actualizado, el panel se refresca
                             orderPanelController.setMesa(mesaSeleccionada, pedido); 
                         }
                         infoLabel.setText("Pedido actualizado remotamente.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error WS Dashboard: " + e.getMessage());
            cargarSoloMesasAsync();
        }
    }

    private void actualizarEstadoMesaEspecifica(String tipoEvento, PedidoMesaDTO pedido) {
        for (Node node : mesasContainer.getChildren()) {
            if (node.getUserData() instanceof MesaTileController) {
                MesaTileController controller = (MesaTileController) node.getUserData();
                MesaDTO mesaBtn = controller.getMesa();

                if (mesaBtn != null && mesaBtn.getIdMesa().equals(pedido.getIdMesa())) {
                    String nuevoEstadoBase = mesaBtn.getEstado();
                    String nuevoEstadoPedido = null;

                    switch (tipoEvento) {
                        case "PEDIDO_CREADO":
                        case "PEDIDO_ACTUALIZADO":
                            nuevoEstadoBase = "OCUPADA";
                            estadoPedidoCache.put(mesaBtn.getIdMesa(), pedido.getEstado());
                            break;
                        case "PEDIDO_LISTO":
                            nuevoEstadoBase = "OCUPADA";
                            nuevoEstadoPedido = "LISTO_PARA_ENTREGAR";
                            estadoPedidoCache.put(mesaBtn.getIdMesa(), "LISTO_PARA_ENTREGAR");
                            break;
                        case "PEDIDO_CERRADO":
                        case "PEDIDO_CANCELADO":
                            nuevoEstadoBase = "DISPONIBLE";
                            estadoPedidoCache.remove(mesaBtn.getIdMesa());
                            break;
                    }
                    controller.actualizarEstadoVisual(nuevoEstadoBase, nuevoEstadoPedido);
                    break;
                }
            }
        }
    }

    // --- Gestión de Datos y UI Admin ---

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
            
            try {
                if (DataCacheService.getInstance().getProductos() != null) {
                    productos = DataCacheService.getInstance().getProductos();
                } else {
                    productos = productoService.getAllProductos();
                    DataCacheService.getInstance().setProductos(productos);
                }
            } catch (Exception e) { errorGeneral = e; }

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

    private void cargarSoloMesasAsync() {
        ThreadManager.getInstance().execute(() -> {
            try {
                List<MesaDTO> mesas = mesaService.getAllMesas();
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                List<PedidoMesaDTO> pedidosActivos = todosLosPedidos.stream()
                        .filter(p -> !"CERRADO".equalsIgnoreCase(p.getEstado()) && !"CANCELADO".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    estadoPedidoCache.clear();
                    for (PedidoMesaDTO pedido : pedidosActivos) {
                        estadoPedidoCache.put(pedido.getIdMesa(), pedido.getEstado());
                    }
                    mostrarMesas(mesas);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear();
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

                    mesasContainer.getChildren().add(mesaNode);
                } catch (IOException e) { e.printStackTrace(); }
            }
        } else {
            mesasContainer.getChildren().add(new Label("Sin mesas."));
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

    // --- Métodos Admin (Crear/Editar) ---
    // Estos métodos se mantienen igual que antes, usando el ProductFormController

    @FXML
    private void handleCrearProducto() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/product-form-view.fxml"));
            Parent formView = loader.load();
            ProductFormController controller = loader.getController();

            Dialog<ProductoDTO> dialog = new Dialog<>();
            dialog.setTitle("Nuevo Producto");
            dialog.getDialogPane().setContent(formView);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) return controller.getProductoResult();
                return null;
            });

            Optional<ProductoDTO> result = dialog.showAndWait();
            result.ifPresent(nuevoProducto -> {
                ThreadManager.getInstance().execute(() -> {
                    try {
                        productoService.crearProducto(nuevoProducto);
                        Platform.runLater(() -> {
                            DataCacheService.getInstance().limpiarCache();
                            cargarDatosIniciales();
                            mostrarAlertaInfo("Éxito", "Producto creado.");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> handleGenericError("Error al crear producto", e));
                    }
                });
            });
        } catch (IOException e) { handleGenericError("Error al abrir formulario", e); }
    }

    private void handleEditarProducto(ProductoDTO producto) {
        try {
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/proyectopos/restauranteappfrontend/product-form-view.fxml"));
             Parent formView = loader.load();
             ProductFormController controller = loader.getController();
             controller.setProducto(producto);

             Dialog<ProductoDTO> dialog = new Dialog<>();
             dialog.setTitle("Editar Producto");
             dialog.getDialogPane().setContent(formView);
             dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

             dialog.setResultConverter(buttonType -> {
                 if (buttonType == ButtonType.OK) return controller.getProductoResult();
                 return null;
             });

             Optional<ProductoDTO> result = dialog.showAndWait();
             result.ifPresent(productoEditado -> {
                 ThreadManager.getInstance().execute(() -> {
                     try {
                         productoService.actualizarProducto(productoEditado.getIdProducto(), productoEditado);
                         Platform.runLater(() -> {
                             DataCacheService.getInstance().limpiarCache();
                             cargarDatosIniciales();
                             infoLabel.setText("Producto actualizado.");
                             infoLabel.getStyleClass().setAll("lbl-success");
                         });
                     } catch (Exception e) {
                         Platform.runLater(() -> handleGenericError("Error al actualizar", e));
                     }
                 });
             });
        } catch (IOException e) { handleGenericError("Error al abrir formulario", e); }
    }

    private void handleEliminarProducto(ProductoDTO producto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar");
        alert.setHeaderText("¿Eliminar " + producto.getNombre() + "?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            ThreadManager.getInstance().execute(() -> {
                try {
                    productoService.eliminarProducto(producto.getIdProducto());
                    Platform.runLater(() -> {
                        DataCacheService.getInstance().limpiarCache();
                        cargarDatosIniciales();
                        infoLabel.setText("Producto eliminado.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> handleGenericError("Error al eliminar", e));
                }
            });
        }
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
         ObservableList<CategoriaDTO> categoriesPadre = categoriasData.stream()
                 .filter(c -> c.getIdCategoriaPadre() == null)
                 .collect(Collectors.toCollection(FXCollections::observableArrayList));
         categoriaPadreComboBox.setItems(categoriesPadre);
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
        ThreadManager.getInstance().execute(() -> {
            try {
                categoriaService.crearCategoria(categoriaACrear);
                Platform.runLater(() -> {
                    infoLabel.setText("Categoría creada.");
                    cargarDatosIniciales();
                });
            } catch(Exception e) {
                 Platform.runLater(() -> handleGenericError("Error creando categoría", e));
            }
        });
    }

    private void configurarBotonesAdmin() {
        String userRole = SessionManager.getInstance().getRole();
        if (adminButtonContainer != null && adminButtonContainer.getParent() != null) {
             ((VBox)adminButtonContainer.getParent()).getChildren().remove(adminButtonContainer);
        }
        adminButtonContainer = null;

        if ("ROLE_ADMIN".equals(userRole)) {
            Button crearProductoBtn = new Button("+ Nuevo Producto");
            crearProductoBtn.getStyleClass().addAll("btn-sm", "btn-info");
            crearProductoBtn.setOnAction(e -> handleCrearProducto());

            Button gestionarCategoriasBtn = new Button("Categorías");
            gestionarCategoriasBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
            gestionarCategoriasBtn.setOnAction(e -> handleGestionarCategorias());

            adminButtonContainer = new HBox(10, crearProductoBtn, gestionarCategoriasBtn);
            adminButtonContainer.setAlignment(Pos.CENTER_LEFT);
            adminButtonContainer.setPadding(new Insets(0, 0, 10, 0));

            try {
                Node scrollPane = productosContainer.getParent().getParent(); 
                if (scrollPane instanceof ScrollPane && scrollPane.getParent() instanceof VBox) {
                    VBox parentVBox = (VBox) scrollPane.getParent();
                    if (!parentVBox.getChildren().contains(adminButtonContainer)) {
                         parentVBox.getChildren().add(1, adminButtonContainer); 
                    }
                }
            } catch (Exception e) { System.err.println("Error UI Admin: " + e.getMessage()); }
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