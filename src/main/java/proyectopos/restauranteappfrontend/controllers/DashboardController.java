package proyectopos.restauranteappfrontend.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton; // ⬅️ IMPORTAR para doble clic
import javafx.scene.layout.TilePane;
import javafx.geometry.Pos;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO; // ⬅️ IMPORTAR
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import com.google.gson.JsonSyntaxException;
// ❗️(Importar PedidoMesaService y DTOs cuando los crees)

import java.io.IOException;
import java.util.List;
import java.util.Optional; // ⬅️ IMPORTAR para diálogo

public class DashboardController {

    // --- Elementos FXML Existentes ---
    @FXML private Label infoLabel;
    @FXML private TilePane mesasContainer;
    @FXML private ListView<CategoriaDTO> categoriasListView;
    @FXML private TableView<ProductoDTO> productosTableView;
    @FXML private TableColumn<ProductoDTO, String> nombreProductoCol;
    @FXML private TableColumn<ProductoDTO, Double> precioProductoCol;
    @FXML private TableColumn<ProductoDTO, String> categoriaProductoCol;

    // --- NUEVOS Elementos FXML para Pedido Actual ---
    @FXML private TableView<DetallePedidoMesaDTO> pedidoActualTableView;
    @FXML private TableColumn<DetallePedidoMesaDTO, String> pedidoNombreCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Integer> pedidoCantidadCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoPrecioCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoSubtotalCol;
    @FXML private Label totalPedidoLabel; // Para mostrar el total
    @FXML private Button crearPedidoButton; // Botón para enviar

    // --- Servicios ---
    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    // private final PedidoMesaService pedidoMesaService = new PedidoMesaService(); // (Cuando lo crees)

    // --- Estado ---
    private MesaDTO mesaSeleccionada = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
    // ⬇️ Lista Observable para el Pedido Actual ⬇️
    private final ObservableList<DetallePedidoMesaDTO> pedidoActualData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        infoLabel.setText("Cargando datos iniciales...");
        configurarTablaProductos();
        configurarContenedorMesas();
        configurarTablaPedidoActual(); // ⬅️ NUEVO: Configurar tabla del pedido
        cargarDatosIniciales();
        configurarSeleccionProducto(); // ⬅️ NUEVO: Añadir listener a tabla productos

        crearPedidoButton.setDisable(true); // Deshabilitar botón hasta que haya mesa y productos

        // Listener para seleccionar categoría (sin cambios)
        categoriasListView.getSelectionModel().selectedItemProperty().addListener(
                // ⬇️ ESTA ES LA EXPRESIÓN LAMBDA COMPLETA ⬇️
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        // Acción al seleccionar una categoría (newValue es el CategoriaDTO seleccionado)
                        infoLabel.setText("Categoría seleccionada: " + newValue.getNombre());
                        // Aquí podrías añadir lógica para filtrar la tabla de productos
                        // por newValue.getIdCategoria() si lo deseas.
                    }
                }
                // ⬆️ FIN DE LA EXPRESIÓN LAMBDA ⬆️
        );
    }

    private void configurarContenedorMesas() { /* ... (sin cambios) ... */ }
    private void configurarTablaProductos() { /* ... (sin cambios) ... */ }

    // --- NUEVO: Configurar Tabla Pedido Actual ---
    private void configurarTablaPedidoActual() {
        pedidoNombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        pedidoCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        pedidoPrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        pedidoSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal")); // Asegúrate que DetallePedidoMesaDTO tenga getSubtotal()
        pedidoActualTableView.setItems(pedidoActualData);

        // Placeholder si no hay items
        pedidoActualTableView.setPlaceholder(new Label("Seleccione una mesa y añada productos"));
    }

    // --- NUEVO: Configurar Selección de Producto ---
    private void configurarSeleccionProducto() {
        productosTableView.setOnMouseClicked(event -> {
            // Detectar doble clic
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                ProductoDTO productoSeleccionado = productosTableView.getSelectionModel().getSelectedItem();
                if (productoSeleccionado != null) {
                    handleSeleccionarProducto(productoSeleccionado);
                }
            }
        });
    }


    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos iniciales..."); // Mensaje inicial
        infoLabel.getStyleClass().setAll("lbl-warning"); // Estilo de carga

        new Thread(() -> {
            List<MesaDTO> mesas = null;
            List<CategoriaDTO> categorias = null;
            List<ProductoDTO> productos = null;
            String errorMessage = null; // Variable para guardar mensaje de error específico
            Exception caughtException = null; // Variable para guardar la excepción

            try {
                System.out.println("Dashboard: Iniciando carga de datos en hilo secundario..."); // DEBUG

                System.out.println("Dashboard: Intentando cargar mesas..."); // DEBUG
                mesas = mesaService.getAllMesas();
                System.out.println("Dashboard: Mesas cargadas OK (" + (mesas != null ? mesas.size() : 0) + " encontradas)."); // DEBUG

                System.out.println("Dashboard: Intentando cargar categorías..."); // DEBUG
                categorias = categoriaService.getAllCategorias();
                System.out.println("Dashboard: Categorías cargadas OK (" + (categorias != null ? categorias.size() : 0) + " encontradas)."); // DEBUG

                System.out.println("Dashboard: Intentando cargar productos..."); // DEBUG
                productos = productoService.getAllProductos();
                System.out.println("Dashboard: Productos cargados OK (" + (productos != null ? productos.size() : 0) + " encontrados)."); // DEBUG

            } catch (HttpClientService.AuthenticationException e) {
                errorMessage = "Error de autenticación: Sesión inválida o expirada.";
                caughtException = e;
                System.err.println("Dashboard: Error de autenticación al cargar datos."); // DEBUG
            } catch (JsonSyntaxException e) { // Captura específica para errores de formato JSON
                errorMessage = "Error al procesar respuesta del servidor (formato JSON inválido).";
                caughtException = e;
                System.err.println("Dashboard: Error de sintaxis JSON."); // DEBUG
            } catch (IOException e) { // Captura errores de red / IO / Status != 2xx
                errorMessage = "Error de conexión con el servidor: " + e.getMessage();
                caughtException = e;
                System.err.println("Dashboard: Error de IO/Red."); // DEBUG
            } catch (InterruptedException e) {
                errorMessage = "Carga de datos interrumpida.";
                caughtException = e;
                Thread.currentThread().interrupt(); // Restablecer estado de interrupción
                System.err.println("Dashboard: Hilo interrumpido."); // DEBUG
            } catch (Exception e) { // Captura cualquier otro error inesperado
                errorMessage = "Error inesperado al cargar datos.";
                caughtException = e;
                System.err.println("Dashboard: Error inesperado general."); // DEBUG
            }

            // --- Actualización de la UI ---
            // Se ejecuta siempre, haya error o no.
            final List<MesaDTO> finalMesas = mesas;
            final List<CategoriaDTO> finalCategorias = categorias;
            final List<ProductoDTO> finalProductos = productos;
            final String finalErrorMessage = errorMessage;
            final Exception finalCaughtException = caughtException;

            Platform.runLater(() -> {
                if (finalErrorMessage == null) {
                    // Éxito: Mostrar todos los datos
                    System.out.println("Dashboard: Actualizando UI con datos cargados."); // DEBUG
                    mostrarMesas(finalMesas);
                    mostrarCategorias(finalCategorias);
                    mostrarProductos(finalProductos);
                    infoLabel.setText("Datos cargados correctamente.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                } else {
                    // Error: Mostrar mensaje de error y limpiar vistas
                    System.out.println("Dashboard: Actualizando UI con mensaje de error: " + finalErrorMessage); // DEBUG
                    infoLabel.setText(finalErrorMessage);
                    infoLabel.getStyleClass().setAll("lbl-danger");
                    // Limpiar vistas para que no se queden en "Cargando..."
                    mesasContainer.getChildren().clear();
                    mesasContainer.getChildren().add(new Label("Error al cargar mesas"));
                    categoriasListView.getItems().clear();
                    categoriasListView.setPlaceholder(new Label("Error al cargar categorías"));
                    productosData.clear();
                    productosTableView.setPlaceholder(new Label("Error al cargar productos"));

                    if (finalCaughtException != null) {
                        // Imprimir el stack trace en la consola para más detalles
                        finalCaughtException.printStackTrace();
                    }
                    if (finalCaughtException instanceof HttpClientService.AuthenticationException) {
                        // TODO: Implementar lógica para cerrar sesión y volver al login
                        System.out.println("Dashboard: Redirigir a Login debido a AuthenticationException.");
                    }
                }
            });

        }).start();
    }    private void mostrarMesas(List<MesaDTO> mesas) { /* ... (sin cambios, usa Botones) ... */ }

    private void handleSeleccionarMesa(MesaDTO mesa) {
        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.mesaSeleccionada = mesa;
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada. Añada productos al pedido.");
            infoLabel.getStyleClass().setAll("lbl-info");
            pedidoActualData.clear(); // Limpiar pedido anterior al seleccionar nueva mesa
            actualizarTotalPedido(); // Poner total a 0.0
            actualizarEstadoCrearPedidoButton(); // Habilitar/deshabilitar botón
            // System.out.println("Mesa seleccionada: " + mesaSeleccionada.getIdMesa()); // Debug
        } else {
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " está " + mesa.getEstado() + ".");
            infoLabel.getStyleClass().setAll("lbl-warning");
            this.mesaSeleccionada = null; // Deseleccionar si no está disponible
            pedidoActualData.clear();
            actualizarTotalPedido();
            actualizarEstadoCrearPedidoButton();
        }
    }

    // --- NUEVO: Manejar Selección de Producto ---
    private void handleSeleccionarProducto(ProductoDTO producto) {
        if (mesaSeleccionada == null) {
            mostrarAlerta("Seleccione una mesa", "Por favor, seleccione una mesa DISPONIBLE antes de añadir productos.");
            return;
        }

        // Preguntar cantidad
        TextInputDialog dialog = new TextInputDialog("1"); // Valor por defecto 1
        dialog.setTitle("Añadir Producto");
        dialog.setHeaderText("Añadir '" + producto.getNombre() + "' al pedido");
        dialog.setContentText("Cantidad:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidadStr -> {
            try {
                int cantidad = Integer.parseInt(cantidadStr);
                if (cantidad > 0) {
                    // Crear el DTO del detalle
                    DetallePedidoMesaDTO detalle = new DetallePedidoMesaDTO();
                    detalle.setIdProducto(producto.getIdProducto());
                    detalle.setNombreProducto(producto.getNombre());
                    detalle.setCantidad(cantidad);
                    detalle.setPrecioUnitario(producto.getPrecio());
                    // ❗️ Necesitas añadir el cálculo del subtotal en el DTO o aquí
                    detalle.setSubtotal(cantidad * producto.getPrecio());

                    // Añadir (o actualizar si ya existe) a la lista del pedido
                    // (Lógica simple, se puede mejorar para agrupar)
                    pedidoActualData.add(detalle);
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton(); // Habilitar botón si hay items

                } else {
                    mostrarAlerta("Cantidad inválida", "La cantidad debe ser un número positivo.");
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Entrada inválida", "Por favor, ingrese un número válido para la cantidad.");
            }
        });
    }

    // --- NUEVO: Actualizar Total del Pedido ---
    private void actualizarTotalPedido() {
        double total = 0.0;
        for (DetallePedidoMesaDTO detalle : pedidoActualData) {
            total += detalle.getSubtotal(); // Asegúrate que getSubtotal() funcione
        }
        totalPedidoLabel.setText(String.format("Total: S/ %.2f", total));
    }

    // --- NUEVO: Actualizar estado del botón Crear Pedido ---
    private void actualizarEstadoCrearPedidoButton() {
        crearPedidoButton.setDisable(mesaSeleccionada == null || pedidoActualData.isEmpty());
    }

    // --- NUEVO: Acción para el botón Crear Pedido ---
    @FXML
    private void handleCrearPedido() {
        if (mesaSeleccionada == null || pedidoActualData.isEmpty()) {
            mostrarAlerta("Pedido incompleto", "Debe seleccionar una mesa y añadir al menos un producto.");
            return;
        }

        infoLabel.setText("Creando pedido...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        crearPedidoButton.setDisable(true); // Deshabilitar mientras se envía

        // 1. Construir el PedidoMesaDTO
        //    (Necesitarás crear esta clase DTO en el frontend)
        /*
        PedidoMesaDTO nuevoPedido = new PedidoMesaDTO();
        nuevoPedido.setIdMesa(mesaSeleccionada.getIdMesa());
        // El idMesero lo asigna el backend basado en el token
        nuevoPedido.setEstado("ABIERTO"); // Estado inicial
        nuevoPedido.setDetalles(new ArrayList<>(pedidoActualData)); // Copiar la lista

        // 2. Llamar al servicio (en hilo separado)
        new Thread(() -> {
            try {
                PedidoMesaDTO pedidoCreado = pedidoMesaService.crearPedido(nuevoPedido);
                Platform.runLater(() -> {
                    infoLabel.setText("Pedido #" + pedidoCreado.getIdPedidoMesa() + " creado exitosamente para Mesa " + mesaSeleccionada.getNumeroMesa());
                    infoLabel.getStyleClass().setAll("lbl-success");
                    // Limpiar estado
                    pedidoActualData.clear();
                    mesaSeleccionada = null;
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton();
                    // Recargar mesas para ver el estado actualizado
                    cargarDatosIniciales();
                });
            } catch (HttpClientService.AuthenticationException e) {
                 Platform.runLater(() -> {
                     handleAuthenticationError(e);
                     crearPedidoButton.setDisable(false); // Rehabilitar en error
                 });
            } catch (Exception e) {
                 Platform.runLater(() -> {
                     handleGenericError("Error al crear el pedido", e);
                     crearPedidoButton.setDisable(false); // Rehabilitar en error
                 });
            }
        }).start();
        */
        // ---- QUITAR ESTE BLOQUE SIMULADO CUANDO IMPLEMENTES EL SERVICIO ----
        infoLabel.setText("Funcionalidad 'Crear Pedido' pendiente de implementar servicio.");
        infoLabel.getStyleClass().setAll("lbl-info");
        // crearPedidoButton.setDisable(false); // Simulación
        // -----------------------------------------------------------------

    }

    // --- NUEVO: Método auxiliar para mostrar alertas ---
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }


    private void mostrarCategorias(List<CategoriaDTO> categorias) { /* ... (sin cambios) ... */ }
    private void mostrarProductos(List<ProductoDTO> productos) { /* ... (sin cambios) ... */ }
    private void handleAuthenticationError(HttpClientService.AuthenticationException e) { /* ... (sin cambios) ... */ }
    private void handleGenericError(String message, Exception e) { /* ... (sin cambios) ... */ }
}