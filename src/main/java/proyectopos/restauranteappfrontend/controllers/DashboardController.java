package proyectopos.restauranteappfrontend.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.animation.Timeline;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.util.SessionManager;

public class DashboardController implements CleanableController {

    @FXML private Label infoLabel;
    @FXML private TilePane mesasContainer;
    @FXML private VBox gestionPedidoPane;
    @FXML private ListView<CategoriaDTO> categoriasListView;
    @FXML private ListView<CategoriaDTO> subCategoriasListView;
    @FXML private TableView<ProductoDTO> productosTableView;
    @FXML private TableColumn<ProductoDTO, String> nombreProductoCol;
    @FXML private TableColumn<ProductoDTO, Double> precioProductoCol;
    @FXML private TableColumn<ProductoDTO, String> categoriaProductoCol;
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

    // --- Contenedor HBox para botones admin (para añadirlo explícitamente) ---
    private HBox adminButtonContainer = null;

    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();

    private MesaDTO mesaSeleccionada = null;
    private PedidoMesaDTO pedidoActual = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> pedidoActualData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> categoriasData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> subCategoriasData = FXCollections.observableArrayList();
    private FilteredList<ProductoDTO> filteredProductos;

    private Timeline refreshTimeline;
    private Map<Long, String> estadoPedidoCache = new HashMap<>();


    @FXML
    public void initialize() {
        infoLabel.setText("Cargando datos iniciales...");
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");

        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);

        categoriasListView.setPlaceholder(new Label("Cargando categorías..."));
        subCategoriasListView.setPlaceholder(new Label("Seleccione categoría"));
        productosTableView.setPlaceholder(new Label("Cargando productos..."));
        mesasContainer.getChildren().clear();
        mesasContainer.getChildren().add(new Label("Cargando mesas..."));

        configurarTablaProductos();
        configurarContenedorMesas();
        configurarTablaPedidoActual();
        configurarSeleccionProducto();
        // --- NO LLAMAR configurarBotonesAdmin aquí ---
        cargarDatosIniciales(); // Ahora cargarDatosIniciales llamará a configurarBotonesAdmin al final

        crearPedidoButton.setDisable(true);

        // --- Lógica de Listas de Categorías (Revisada y Simplificada) ---
        subCategoriasListView.setItems(subCategoriasData);

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
                }
        );

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
                }
        );
    }

    // --- Métodos de polling (sin cambios) ---
    private void setupAutoRefreshPedidos() { /* ... */ }
    private void actualizarEstadosPedidosAsync() { /* ... */ }
    private void procesarActualizacionEstados(List<PedidoMesaDTO> pedidosActivos) { /* ... */ }
    private void cargarSoloMesasAsync() { /* ... */ }

    // --- Métodos de configuración UI ---
    private void configurarContenedorMesas() { /* Sin cambios */ }
    private void configurarTablaProductos() {
        nombreProductoCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        precioProductoCol.setCellValueFactory(new PropertyValueFactory<>("precio"));
        categoriaProductoCol.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));
        filteredProductos = new FilteredList<>(productosData, p -> true);
        productosTableView.setItems(filteredProductos);
     }
    private void configurarTablaPedidoActual() {
        pedidoNombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        pedidoCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        pedidoPrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        pedidoSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        pedidoActualTableView.setItems(pedidoActualData);
        pedidoActualTableView.setPlaceholder(new Label("Añada productos (doble clic)"));
    }
    private void configurarSeleccionProducto() {
         productosTableView.setOnMouseClicked(event -> {
             if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                 ProductoDTO productoSeleccionado = productosTableView.getSelectionModel().getSelectedItem();
                 if (productoSeleccionado != null) {
                     handleSeleccionarProducto(productoSeleccionado);
                 }
             }
         });
     }

    // --- MÉTODO CORREGIDO PARA AÑADIR BOTONES ADMIN ---
    private void configurarBotonesAdmin() {
        String userRole = SessionManager.getInstance().getRole();
        // Limpiar contenedor previo si existe
        if (adminButtonContainer != null && adminButtonContainer.getParent() != null) {
             ((VBox)adminButtonContainer.getParent()).getChildren().remove(adminButtonContainer);
        }
        adminButtonContainer = null; // Resetear

        if (userRole == null || !userRole.equals("ROLE_ADMIN")) {
            System.out.println("Ocultando botones de admin. Rol de usuario: " + userRole);
            return;
        }

        System.out.println("Intentando configurar botones de admin...");
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

        // Intentar añadir el contenedor al VBox padre de la tabla de productos
        try {
            // Asumiendo que el FXML tiene: VBox -> Label ("Productos") -> TableView
            Node parent = productosTableView.getParent();
            if (parent instanceof VBox) {
                VBox parentVBox = (VBox) parent;
                // Buscar el índice de la tabla para insertar antes
                int tableViewIndex = parentVBox.getChildren().indexOf(productosTableView);
                if (tableViewIndex > 0) { // Insertar antes de la tabla si se encuentra
                    // Insertar solo si no existe ya
                    if (parentVBox.getChildren().stream().noneMatch(node -> node == adminButtonContainer)) {
                       parentVBox.getChildren().add(tableViewIndex -1, adminButtonContainer); // Insertar justo antes de la tabla, después del label
                       System.out.println("Botones de admin añadidos correctamente.");
                    }
                } else { // Si no se encuentra la tabla o está al principio, añadir al final (fallback)
                     if (parentVBox.getChildren().stream().noneMatch(node -> node == adminButtonContainer)) {
                        parentVBox.getChildren().add(adminButtonContainer);
                        System.out.println("Botones de admin añadidos (fallback al final).");
                     }
                }
            } else {
                System.err.println("Advertencia: No se pudo encontrar el VBox padre esperado para la tabla de productos. Parent: " + (parent != null ? parent.getClass().getName() : "null"));
            }
        } catch (Exception e) {
            System.err.println("Error crítico al intentar añadir botones de admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- cargarDatosIniciales (Llama a configurarBotonesAdmin al final) ---
    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos iniciales...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        setUIDisabledDuringLoad(true);

        new Thread(() -> {
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
                StringBuilder errorMessages = new StringBuilder();
                HttpClientService.AuthenticationException authException = null;

                if (finalMesas != null) {
                    procesarActualizacionEstados(finalPedidosActivos != null ? finalPedidosActivos : new ArrayList<>());
                    mostrarMesas(finalMesas);
                } else {
                    huboErrorGeneral = true;
                    mesasContainer.getChildren().clear();
                    mesasContainer.getChildren().add(new Label("Error al cargar mesas."));
                    if (finalErrorMesas != null) { errorMessages.append("Mesas: ").append(finalErrorMesas.getMessage()).append("\n"); if (finalErrorMesas instanceof HttpClientService.AuthenticationException && authException == null) authException = (HttpClientService.AuthenticationException) finalErrorMesas; }
                }
                if (finalCategorias != null) {
                    mostrarCategorias(finalCategorias);
                } else {
                    huboErrorGeneral = true;
                    categoriasListView.setPlaceholder(new Label("Error al cargar categorías."));
                    subCategoriasListView.setPlaceholder(new Label("Error"));
                    if (finalErrorCategorias != null) { errorMessages.append("Categorías: ").append(finalErrorCategorias.getMessage()).append("\n"); if (finalErrorCategorias instanceof HttpClientService.AuthenticationException && authException == null) authException = (HttpClientService.AuthenticationException) finalErrorCategorias; }
                }
                if (finalProductos != null) {
                    mostrarProductos(finalProductos);
                } else {
                    huboErrorGeneral = true;
                    productosTableView.setPlaceholder(new Label("Error al cargar productos."));
                    if (finalErrorProductos != null) { errorMessages.append("Productos: ").append(finalErrorProductos.getMessage()).append("\n"); if (finalErrorProductos instanceof HttpClientService.AuthenticationException && authException == null) authException = (HttpClientService.AuthenticationException) finalErrorProductos; }
                }
                if (finalErrorPedidos != null) { errorMessages.append("Pedidos iniciales: ").append(finalErrorPedidos.getMessage()).append("\n"); if (finalErrorPedidos instanceof HttpClientService.AuthenticationException && authException == null) authException = (HttpClientService.AuthenticationException) finalErrorPedidos; }

                if (huboErrorGeneral) {
                    infoLabel.setText("Error al cargar algunos datos iniciales.");
                    infoLabel.getStyleClass().setAll("lbl-danger");
                    System.err.println("Errores durante la carga inicial:\n" + errorMessages.toString());
                    if (authException != null) { handleAuthenticationError(authException); }
                } else {
                    infoLabel.setText("Datos cargados. Actualización automática iniciada.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                    setupAutoRefreshPedidos();
                }
                
                // --- LLAMAR A CONFIGURAR BOTONES ADMIN AQUÍ ---
                configurarBotonesAdmin(); 
                // --- FIN LLAMADA ---
                
                setUIDisabledDuringLoad(false); // Habilitar UI
            });
        }).start();
    }

    private void setUIDisabledDuringLoad(boolean disabled) {
         if (mesasContainer != null) mesasContainer.setDisable(disabled);
         if (categoriasListView != null) categoriasListView.setDisable(disabled);
         if (subCategoriasListView != null) subCategoriasListView.setDisable(disabled);
         if (productosTableView != null) productosTableView.setDisable(disabled);
         if (gestionPedidoPane != null) gestionPedidoPane.setDisable(disabled);
    }

    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear();
        if (mesas != null && !mesas.isEmpty()) {
            for (MesaDTO mesa : mesas) {
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
        pedidoActualData.clear();
        actualizarTotalPedido();
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
        if (filteredProductos != null) filteredProductos.setPredicate(p -> true);

        gestionPedidoPane.setVisible(true);
        gestionPedidoPane.setManaged(true);

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.pedidoActual = null;
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Nueva Orden)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada. Añada productos al pedido.");
            infoLabel.getStyleClass().setAll("lbl-info");
            actualizarEstadoCrearPedidoButton();
            pedidoActualTableView.setPlaceholder(new Label("Añada productos (doble clic)"));
        } else if ("OCUPADA".equals(mesa.getEstado())) {
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Orden Activa)");
            infoLabel.setText("Cargando pedido activo de Mesa " + mesa.getNumeroMesa() + "...");
            infoLabel.getStyleClass().setAll("lbl-warning");
            pedidoActualTableView.setPlaceholder(new Label("Cargando items..."));

            new Thread(() -> {
                try {
                    PedidoMesaDTO pedidoCargado = pedidoMesaService.getPedidoActivoPorMesa(mesa.getIdMesa());
                    Platform.runLater(() -> {
                        this.pedidoActual = pedidoCargado;
                        if (pedidoCargado.getDetalles() != null) {
                            pedidoActualData.addAll(pedidoCargado.getDetalles());
                        }
                        pedidoActualTableView.setPlaceholder(new Label("Añada productos (doble clic)"));
                        actualizarTotalPedido();
                        actualizarEstadoCrearPedidoButton();
                        infoLabel.setText("Pedido de Mesa " + mesa.getNumeroMesa() + " cargado. Puede añadir más productos.");
                        infoLabel.getStyleClass().setAll("lbl-info");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al cargar pedido activo", e);
                        mostrarAlertaError("Error", "No se pudo cargar el pedido activo para la mesa " + mesa.getNumeroMesa() + ".");
                        pedidoActualTableView.setPlaceholder(new Label("Error al cargar items."));
                        gestionPedidoPane.setVisible(false);
                        gestionPedidoPane.setManaged(false);
                        this.mesaSeleccionada = null;
                    });
                }
            }).start();
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
                    Optional<DetallePedidoMesaDTO> existente = pedidoActualData.stream()
                        .filter(d -> d.getIdProducto().equals(producto.getIdProducto()))
                        .findFirst();
                    if (existente.isPresent()) {
                        DetallePedidoMesaDTO detalle = existente.get();
                        detalle.setCantidad(detalle.getCantidad() + cantidad);
                        pedidoActualTableView.getColumns().get(0).setVisible(false);
                        pedidoActualTableView.getColumns().get(0).setVisible(true);
                    } else {
                        DetallePedidoMesaDTO detalle = new DetallePedidoMesaDTO();
                        detalle.setIdProducto(producto.getIdProducto());
                        detalle.setNombreProducto(producto.getNombre());
                        detalle.setCantidad(cantidad);
                        detalle.setPrecioUnitario(producto.getPrecio());
                        detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
                        pedidoActualData.add(detalle);
                    }
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton();
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
        new Thread(() -> {
            try {
                CategoriaDTO categoriaCreada = categoriaService.crearCategoria(categoriaACrear);
                Platform.runLater(() -> {
                    infoLabel.setText("Categoría '" + categoriaCreada.getNombre() + "' creada.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                    cargarDatosIniciales(); // Recargar todo para ver cambios
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleGenericError("Error al crear la categoría", e));
            }
        }).start();
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
        grid.add(new Label("Categoría:"), 0, 3); grid.add(comboCategoriaPadre, 1, 3);
        grid.add(new Label("Subcategoría:"), 0, 4); grid.add(comboSubcategoria, 1, 4);
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
                    ProductoDTO np = new ProductoDTO(); np.setNombre(nombreField.getText().trim());
                    np.setDescripcion(descripcionField.getText().trim());
                    double precio = Double.parseDouble(precioField.getText().trim());
                    if (precio <= 0) throw new NumberFormatException("Precio debe ser positivo");
                    np.setPrecio(precio); np.setIdCategoria(comboSubcategoria.getValue().getIdCategoria());
                    return np;
                } catch (NumberFormatException e) { mostrarAlerta("Datos Inválidos", "El precio debe ser un número positivo."); return null; }
            } return null;
        });
        Optional<ProductoDTO> result = dialog.showAndWait();
        result.ifPresent(productoACrear -> {
            if (productoACrear == null) return;
            infoLabel.setText("Creando producto '" + productoACrear.getNombre() + "'...");
            infoLabel.getStyleClass().setAll("lbl-warning");
            new Thread(() -> {
                try {
                    ProductoDTO productoCreado = productoService.crearProducto(productoACrear);
                    Platform.runLater(() -> {
                        infoLabel.setText("Producto '" + productoCreado.getNombre() + "' creado.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        cargarDatosIniciales(); // Recargar para ver el nuevo producto
                    });
                } catch (Exception e) { Platform.runLater(() -> handleGenericError("Error al crear el producto", e)); }
            }).start();
        });
    }

    private void actualizarTotalPedido() {
        double subtotal = 0.0;
        for (DetallePedidoMesaDTO detalle : pedidoActualData) {
            // Asegurar recálculo
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
        boolean deshabilitar = (mesaSeleccionada == null || pedidoActualData.isEmpty());
        crearPedidoButton.setDisable(deshabilitar);
        if (pedidoActual == null) {
            crearPedidoButton.setText("Enviar Pedido a Cocina");
        } else {
            crearPedidoButton.setText("Actualizar Pedido en Cocina");
        }
    }

    @FXML
    private void handleEnviarPedido() {
        if (mesaSeleccionada == null || pedidoActualData.isEmpty()) {
            mostrarAlerta("Pedido incompleto", "Debe seleccionar una mesa y añadir al menos un producto.");
            return;
        }
        infoLabel.setText("Enviando pedido...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        crearPedidoButton.setDisable(true); // Deshabilitar mientras se envía

        PedidoMesaDTO pedidoDTO = new PedidoMesaDTO();
        pedidoDTO.setIdMesa(mesaSeleccionada.getIdMesa());
        pedidoDTO.setEstado("ABIERTO");
        pedidoActualData.forEach(d -> d.setSubtotal(d.getCantidad() * d.getPrecioUnitario()));
        pedidoDTO.setDetalles(new ArrayList<>(pedidoActualData));

        if (this.pedidoActual == null) {
            // --- CREAR PEDIDO ---
            new Thread(() -> {
                try {
                    PedidoMesaDTO pedidoCreado = pedidoMesaService.crearPedido(pedidoDTO);
                    Platform.runLater(() -> {
                        infoLabel.setText("Pedido #" + pedidoCreado.getIdPedidoMesa() + " creado para Mesa " + mesaSeleccionada.getNumeroMesa());
                        infoLabel.getStyleClass().setAll("lbl-success");
                        resetearPanelPedido();
                        cargarDatosIniciales();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al crear el pedido", e);
                        // --- CORRECCIÓN: Rehabilitar botón en caso de error ---
                        crearPedidoButton.setDisable(false);
                        // --- FIN CORRECCIÓN ---
                    });
                }
            }).start();
        } else {
            // --- ACTUALIZAR PEDIDO ---
            Long idPedidoAActualizar = this.pedidoActual.getIdPedidoMesa();
            new Thread(() -> {
                try {
                    PedidoMesaDTO pedidoActualizado = pedidoMesaService.actualizarPedido(idPedidoAActualizar, pedidoDTO);
                    Platform.runLater(() -> {
                        infoLabel.setText("Pedido #" + pedidoActualizado.getIdPedidoMesa() + " (Mesa " + mesaSeleccionada.getNumeroMesa() + ") actualizado.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        resetearPanelPedido();
                        cargarDatosIniciales();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al actualizar el pedido", e);
                         // --- CORRECCIÓN: Rehabilitar botón en caso de error ---
                        crearPedidoButton.setDisable(false);
                        // --- FIN CORRECCIÓN ---
                    });
                }
            }).start();
        }
    }

    private void resetearPanelPedido() {
        this.mesaSeleccionada = null;
        this.pedidoActual = null;
        pedidoActualData.clear();
        actualizarTotalPedido();
        actualizarEstadoCrearPedidoButton();
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);
        categoriasListView.getSelectionModel().clearSelection();
        subCategoriasListView.getSelectionModel().clearSelection();
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
        productosData.clear();
        if (productos != null && !productos.isEmpty()) {
            productosData.addAll(productos);
             productosTableView.setPlaceholder(null);
        } else {
             productosTableView.setPlaceholder(new Label(productos == null ? "Error al cargar productos." : "No se encontraron productos."));
        }
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
        }
        productosTableView.refresh(); // Forzar refresco
    }


    private void handleAuthenticationError(HttpClientService.AuthenticationException e) {
        infoLabel.setText(e.getMessage());
        infoLabel.getStyleClass().setAll("lbl-danger");
        System.err.println("Error de autenticación, se debería redirigir al login.");
        categoriasListView.setPlaceholder(new Label("Error de autenticación."));
        productosTableView.setPlaceholder(new Label("Error de autenticación."));
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
        if (productosTableView.getItems().isEmpty()) productosTableView.setPlaceholder(new Label("Error al cargar."));
         if (mesasContainer.getChildren().size() <=1 && mesasContainer.getChildren().get(0) instanceof Label) mesasContainer.getChildren().set(0, new Label("Error al cargar."));
        mostrarAlertaError("Error", errorMessage);
    }

    @Override
    public void cleanup() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            System.out.println("Timeline de actualización de pedidos detenido.");
        }
    }
}