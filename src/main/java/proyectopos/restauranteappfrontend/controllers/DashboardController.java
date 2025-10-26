package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
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

// --- IMPLEMENTAR INTERFAZ ---
public class DashboardController implements CleanableController {
// --- FIN IMPLEMENTACIÓN ---

    // --- Elementos FXML (Sin cambios) ---
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

    // --- Servicios (Sin cambios) ---
    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();

    // --- Estado (Sin cambios) ---
    private MesaDTO mesaSeleccionada = null;
    private PedidoMesaDTO pedidoActual = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> pedidoActualData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> categoriasData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> subCategoriasData = FXCollections.observableArrayList();
    private FilteredList<ProductoDTO> filteredProductos;

    // --- ¡¡NUEVO ESTADO PARA POLLING!! ---
    private Timeline refreshTimeline;
    private Map<Long, String> estadoPedidoCache = new HashMap<>(); // Cache para Mesa ID -> Estado Pedido


    @FXML
    public void initialize() {
        infoLabel.setText("Cargando datos iniciales...");
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");

        gestionPedidoPane.setVisible(false);
        gestionPedidoPane.setManaged(false);

        configurarTablaProductos();
        configurarContenedorMesas();
        configurarTablaPedidoActual();
        cargarDatosIniciales(); // Esto ahora también inicia el polling
        configurarSeleccionProducto();

        configurarBotonesAdmin();

        crearPedidoButton.setDisable(true);

        // --- Lógica de Listas de Categorías (Sin cambios) ---
        subCategoriasListView.setItems(subCategoriasData);
        subCategoriasListView.setPlaceholder(new Label("Seleccione una categoría principal"));

        categoriasListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, categoriaSeleccionada) -> {
                    subCategoriasData.clear();
                    subCategoriasListView.getSelectionModel().clearSelection();
                    if (filteredProductos != null) {
                        filteredProductos.setPredicate(p -> true);
                    }
                    if (categoriaSeleccionada != null) {
                        infoLabel.setText("Categoría: " + categoriaSeleccionada.getNombre());
                        List<CategoriaDTO> subcategorias = categoriasData.stream()
                                .filter(c -> categoriaSeleccionada.getIdCategoria().equals(c.getIdCategoriaPadre()))
                                .collect(Collectors.toList());
                        subCategoriasData.addAll(subcategorias);
                        if(subcategorias.isEmpty()){
                            subCategoriasListView.setPlaceholder(new Label("No hay subcategorías"));
                        }
                    } else {
                        subCategoriasListView.setPlaceholder(new Label("Seleccione una categoría principal"));
                    }
                }
        );

        subCategoriasListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, subCategoriaSeleccionada) -> {
                    if (filteredProductos == null) return;
                    if (subCategoriaSeleccionada != null) {
                        infoLabel.setText("Subcategoría: " + subCategoriaSeleccionada.getNombre());
                        filteredProductos.setPredicate(producto ->
                                producto.getIdCategoria().equals(subCategoriaSeleccionada.getIdCategoria())
                        );
                    } else {
                        CategoriaDTO catPrincipal = categoriasListView.getSelectionModel().getSelectedItem();
                        if (catPrincipal != null) {
                            filteredProductos.setPredicate(p -> true);
                            infoLabel.setText("Categoría: " + catPrincipal.getNombre());
                        } else {
                            filteredProductos.setPredicate(p -> true);
                        }
                    }
                }
        );
    }

    // --- ¡¡NUEVO MÉTODO PARA POLLING!! ---
    private void setupAutoRefreshPedidos() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(20), event -> { // Revisar cada 20 segundos
                System.out.println("Actualizando estados de pedidos para mesero...");
                actualizarEstadosPedidosAsync();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        System.out.println("Timeline de actualización de pedidos iniciado.");
    }

    // --- ¡¡NUEVO MÉTODO PARA POLLING (LLAMADA ASÍNCRONA)!! ---
    private void actualizarEstadosPedidosAsync() {
        new Thread(() -> {
            try {
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                // Filtramos solo los pedidos que están activos (no cerrados/cancelados)
                List<PedidoMesaDTO> pedidosActivos = todosLosPedidos.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()) ||
                                     "EN_COCINA".equalsIgnoreCase(p.getEstado()) ||
                                     "LISTO_PARA_ENTREGAR".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());

                // Actualizamos el cache y detectamos cambios en el hilo de UI
                Platform.runLater(() -> procesarActualizacionEstados(pedidosActivos));

            } catch (Exception e) {
                 // Manejar error silenciosamente o mostrar un aviso discreto
                 System.err.println("Error al actualizar estados de pedidos: " + e.getMessage());
                 // Platform.runLater(() -> infoLabel.setText("Error al actualizar estados.")); // Opcional
            }
        }).start();
    }

    // --- ¡¡NUEVO MÉTODO PARA POLLING (PROCESAMIENTO EN UI)!! ---
    private void procesarActualizacionEstados(List<PedidoMesaDTO> pedidosActivos) {
        boolean necesitaRefrescarVistaMesas = false;
        Map<Long, String> nuevoCache = new HashMap<>();

        for (PedidoMesaDTO pedido : pedidosActivos) {
            Long idMesa = pedido.getIdMesa();
            String estadoNuevo = pedido.getEstado();
            nuevoCache.put(idMesa, estadoNuevo); // Construir el nuevo cache

            String estadoAnterior = estadoPedidoCache.get(idMesa);

            // Detectar si el estado CAMBIÓ o si es un pedido nuevo en el cache
            if (estadoAnterior == null || !estadoAnterior.equalsIgnoreCase(estadoNuevo)) {
                System.out.println("Cambio detectado para Mesa ID " + idMesa + ": " + estadoAnterior + " -> " + estadoNuevo);
                necesitaRefrescarVistaMesas = true; // Marcamos que necesitamos redibujar
            }
        }

        // Detectar si alguna mesa ya no tiene pedido activo (se cerró/canceló)
        for (Long idMesaEnCache : estadoPedidoCache.keySet()) {
             if (!nuevoCache.containsKey(idMesaEnCache)) {
                  System.out.println("Mesa ID " + idMesaEnCache + " ya no tiene pedido activo. Refrescando.");
                  necesitaRefrescarVistaMesas = true;
                  break; // Ya sabemos que hay que refrescar
             }
        }


        // Actualizar el cache
        this.estadoPedidoCache = nuevoCache;

        // Si hubo algún cambio, redibujar las mesas
        if (necesitaRefrescarVistaMesas) {
            System.out.println("Refrescando vista de mesas debido a cambios de estado.");
            // Volvemos a cargar las mesas, pero mantenemos los productos/categorías
            cargarSoloMesasAsync();
        }
    }

    // --- ¡¡NUEVO MÉTODO PARA RECARGAR SOLO MESAS!! ---
    private void cargarSoloMesasAsync() {
         new Thread(() -> {
            try {
                List<MesaDTO> mesas = mesaService.getAllMesas();
                Platform.runLater(() -> mostrarMesas(mesas)); // Llama a mostrarMesas modificado
            } catch (Exception e) {
                System.err.println("Error al recargar solo mesas: " + e.getMessage());
                 Platform.runLater(() -> infoLabel.setText("Error al actualizar mesas."));
            }
        }).start();
    }


    private void configurarContenedorMesas() { /* Sin cambios */ }
    private void configurarTablaProductos() { /* Sin cambios */ }
    private void configurarTablaPedidoActual() { /* Sin cambios */ }
    private void configurarSeleccionProducto() { /* Sin cambios */ }
    private void configurarBotonesAdmin() { /* Sin cambios */ }


    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos iniciales...");
        infoLabel.getStyleClass().setAll("lbl-warning");

        new Thread(() -> {
            List<MesaDTO> mesas = null;
            List<CategoriaDTO> categorias = null;
            List<ProductoDTO> productos = null;
            List<PedidoMesaDTO> pedidosActivosInicial = null; // Para llenar el cache inicial
            String errorMessage = null;
            Exception caughtException = null;

            try {
                mesas = mesaService.getAllMesas();
                categorias = categoriaService.getAllCategorias();
                productos = productoService.getAllProductos();

                // --- CARGA INICIAL DE ESTADOS DE PEDIDOS ---
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                pedidosActivosInicial = todosLosPedidos.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()) ||
                                     "EN_COCINA".equalsIgnoreCase(p.getEstado()) ||
                                     "LISTO_PARA_ENTREGAR".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());
                // --- FIN CARGA INICIAL ---

            } catch (HttpClientService.AuthenticationException e) {
                errorMessage = "Error de autenticación: Sesión inválida o expirada.";
                caughtException = e;
            } catch (JsonSyntaxException e) {
                errorMessage = "Error al procesar respuesta del servidor (formato JSON inválido).";
                caughtException = e;
            } catch (IOException e) {
                errorMessage = "Error de conexión con el servidor: " + e.getMessage();
                caughtException = e;
            } catch (InterruptedException e) {
                errorMessage = "Carga de datos interrumpida.";
                caughtException = e;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                errorMessage = "Error inesperado al cargar datos.";
                caughtException = e;
            }


            final List<MesaDTO> finalMesas = mesas;
            final List<CategoriaDTO> finalCategorias = categorias;
            final List<ProductoDTO> finalProductos = productos;
            final List<PedidoMesaDTO> finalPedidosActivos = pedidosActivosInicial; // Pasar a UI thread
            final String finalErrorMessage = errorMessage;
            final Exception finalCaughtException = caughtException;

            Platform.runLater(() -> {
                if (finalErrorMessage == null) {
                    // --- LLENAR CACHE INICIAL ANTES DE MOSTRAR MESAS ---
                    procesarActualizacionEstados(finalPedidosActivos != null ? finalPedidosActivos : new ArrayList<>()); // Evitar NPE
                    // --- FIN LLENAR CACHE ---

                    mostrarMesas(finalMesas); // Ahora mostrarMesas usará el cache
                    mostrarCategorias(finalCategorias);
                    mostrarProductos(finalProductos);
                    infoLabel.setText("Datos cargados. Actualización automática iniciada.");
                    infoLabel.getStyleClass().setAll("lbl-success");

                    // --- INICIAR POLLING DESPUÉS DE CARGA EXITOSA ---
                    setupAutoRefreshPedidos();
                    // --- FIN INICIAR POLLING ---

                } else {
                    infoLabel.setText(finalErrorMessage);
                    infoLabel.getStyleClass().setAll("lbl-danger");
                    mesasContainer.getChildren().clear();
                    mesasContainer.getChildren().add(new Label("Error al cargar mesas"));
                    categoriasListView.getItems().clear();
                    categoriasListView.setPlaceholder(new Label("Error al cargar categorías"));
                    subCategoriasListView.getItems().clear();
                    subCategoriasListView.setPlaceholder(new Label("Error al cargar"));
                    productosData.clear();
                    productosTableView.setPlaceholder(new Label("Error al cargar productos"));

                    if (finalCaughtException != null) {
                        finalCaughtException.printStackTrace();
                    }
                    if (finalCaughtException instanceof HttpClientService.AuthenticationException) {
                        handleAuthenticationError((HttpClientService.AuthenticationException) finalCaughtException);
                    } else {
                        handleGenericError(finalErrorMessage, finalCaughtException);
                    }
                }
            });
        }).start();
    }

    // --- ¡¡MÉTODO MODIFICADO PARA USAR CACHE Y MARCAR PEDIDOS LISTOS!! ---
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

                // --- LÓGICA DE ESTILO MODIFICADA ---
                String estadoPedidoParaMesa = estadoPedidoCache.get(mesa.getIdMesa());

                switch (mesa.getEstado()) {
                    case "DISPONIBLE":
                        mesaButton.getStyleClass().add("mesa-libre");
                        estadoMesaText.setText("Libre");
                        mesaButton.setOnAction(event -> handleSeleccionarMesa(mesa));
                        break;
                    case "OCUPADA":
                        // Ahora verificamos el estado del pedido desde el cache
                        if ("LISTO_PARA_ENTREGAR".equalsIgnoreCase(estadoPedidoParaMesa)) {
                             mesaButton.getStyleClass().add("mesa-pagando"); // Usar estilo 'pagando' (amarillo) para listo
                             estadoMesaText.setText("¡LISTO!"); // Indicar que está listo
                             // Podrías añadir un efecto de parpadeo aquí si quieres más énfasis
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
                // --- FIN LÓGICA DE ESTILO MODIFICADA ---

                // Ajustar color de texto
                if ("LISTO_PARA_ENTREGAR".equalsIgnoreCase(estadoPedidoParaMesa)) { // Chequear estado pedido aquí también
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
            mesasContainer.getChildren().add(new Label("No se encontraron mesas."));
        }
    }


    private void handleSeleccionarMesa(MesaDTO mesa) {
        // Limpiar estado anterior
        this.mesaSeleccionada = mesa;
        this.pedidoActual = null;
        pedidoActualData.clear();
        actualizarTotalPedido();

        gestionPedidoPane.setVisible(true);
        gestionPedidoPane.setManaged(true);

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.pedidoActual = null; // Es un pedido nuevo
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Nueva Orden)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada. Añada productos al pedido.");
            infoLabel.getStyleClass().setAll("lbl-info");
            actualizarEstadoCrearPedidoButton();
        } else if ("OCUPADA".equals(mesa.getEstado())) {
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Orden Activa)");
            infoLabel.setText("Cargando pedido activo de Mesa " + mesa.getNumeroMesa() + "...");
            infoLabel.getStyleClass().setAll("lbl-warning");

            // --- Llamar al servicio para obtener el pedido activo ---
            new Thread(() -> {
                try {
                    PedidoMesaDTO pedidoCargado = pedidoMesaService.getPedidoActivoPorMesa(mesa.getIdMesa());

                    Platform.runLater(() -> {
                        this.pedidoActual = pedidoCargado; // Guardar el pedido que estamos editando
                        if (pedidoCargado.getDetalles() != null) {
                            pedidoActualData.addAll(pedidoCargado.getDetalles());
                        }
                        actualizarTotalPedido();
                        actualizarEstadoCrearPedidoButton();
                        infoLabel.setText("Pedido de Mesa " + mesa.getNumeroMesa() + " cargado. Puede añadir más productos.");
                        infoLabel.getStyleClass().setAll("lbl-info");
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al cargar pedido activo", e);
                        mostrarAlertaError("Error", "No se pudo cargar el pedido activo para la mesa " + mesa.getNumeroMesa() + ".");
                        gestionPedidoPane.setVisible(false);
                        gestionPedidoPane.setManaged(false);
                        this.mesaSeleccionada = null;
                    });
                }
            }).start();

        } else {
            // Otros estados (Reservada)
            this.mesaSeleccionada = null;
            this.pedidoActual = null;
            mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " está " + mesa.getEstado() + ".");
            infoLabel.getStyleClass().setAll("lbl-warning");
            gestionPedidoPane.setVisible(false); // Ocultar
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
                        pedidoActualTableView.refresh(); // Mejor forma de refrescar
                    } else {
                        DetallePedidoMesaDTO detalle = new DetallePedidoMesaDTO();
                        detalle.setIdProducto(producto.getIdProducto());
                        detalle.setNombreProducto(producto.getNombre());
                        detalle.setCantidad(cantidad);
                        detalle.setPrecioUnitario(producto.getPrecio());
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

    @FXML private void handleGestionarCategorias() { /* Sin cambios */ }
    private void llamarCrearCategoriaApi(CategoriaDTO categoriaACrear) { /* Sin cambios */ }
    @FXML private void handleCrearProducto() { /* Sin cambios */ }
    private void actualizarTotalPedido() { /* Sin cambios */ }
    private void actualizarEstadoCrearPedidoButton() { /* Sin cambios */ }
    @FXML private void handleEnviarPedido() { /* Sin cambios */ }
    private void resetearPanelPedido() { /* Sin cambios */ }
    private void mostrarAlerta(String titulo, String contenido) { /* Sin cambios */ }
    private void mostrarAlertaError(String titulo, String contenido) { /* Sin cambios */ }
    private void mostrarCategorias(List<CategoriaDTO> categorias) { /* Sin cambios */ }
    private void mostrarProductos(List<ProductoDTO> productos) { /* Sin cambios */ }
    private void handleAuthenticationError(HttpClientService.AuthenticationException e) { /* Sin cambios */ }
    private void handleGenericError(String message, Exception e) { /* Sin cambios */ }

    // --- ¡¡NUEVO MÉTODO DE CLEANUP!! ---
    @Override
    public void cleanup() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            System.out.println("Timeline de actualización de pedidos detenido.");
        }
    }
}