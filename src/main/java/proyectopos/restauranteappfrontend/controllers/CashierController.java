package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class CashierController {

    // --- Tabla Izquierda (Pedidos Listos) ---
    @FXML private TableView<PedidoMesaDTO> pedidosListosTable;
    @FXML private TableColumn<PedidoMesaDTO, Integer> mesaCol;
    @FXML private TableColumn<PedidoMesaDTO, String> meseroCol;
    @FXML private TableColumn<PedidoMesaDTO, String> horaCol;
    @FXML private TableColumn<PedidoMesaDTO, Double> totalCol;
    @FXML private TableColumn<PedidoMesaDTO, String> estadoPedidoCol;

    // --- Panel Derecho (Detalle Pedido) ---
    @FXML private VBox detallePedidoPane;
    @FXML private Label pedidoSeleccionadoLabel;
    @FXML private TableView<DetallePedidoMesaDTO> detallePedidoTable;
    @FXML private TableColumn<DetallePedidoMesaDTO, String> detalleProductoCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Integer> detalleCantidadCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> detallePrecioCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> detalleSubtotalCol;
    @FXML private Label subTotalDetalleLabel;
    @FXML private Label igvDetalleLabel;
    @FXML private Label totalDetalleLabel;
    @FXML private Button cerrarPedidoButton;

    // --- Label de Estado General ---
    @FXML private Label statusLabelCashier;

    // --- Servicios y Estado ---
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    private ObservableList<PedidoMesaDTO> pedidosListData = FXCollections.observableArrayList();
    private ObservableList<DetallePedidoMesaDTO> detallePedidoData = FXCollections.observableArrayList();
    private PedidoMesaDTO pedidoSeleccionado = null;

    // --- Formateadores ---
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));


    @FXML
    public void initialize() {
        statusLabelCashier.setText("Inicializando vista de caja...");
        configurarTablas();
        cargarPedidosListos();

        pedidosListosTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> mostrarDetallePedido(newValue)
        );
    }

    private void configurarTablas() {
        // Tabla Izquierda (Pedidos Listos)
        mesaCol.setCellValueFactory(new PropertyValueFactory<>("numeroMesa"));
        meseroCol.setCellValueFactory(new PropertyValueFactory<>("nombreMesero"));
        horaCol.setCellValueFactory(cellData -> {
             PedidoMesaDTO pedido = cellData.getValue();
             String horaStr = "";
             if (pedido != null && pedido.getFechaHoraCreacion() != null) {
                 try {
                     LocalDateTime ldt = LocalDateTime.parse(pedido.getFechaHoraCreacion());
                     horaStr = ldt.format(TIME_FORMATTER);
                 } catch (Exception e) {
                      try { horaStr = pedido.getFechaHoraCreacion().length() >= 16 ? pedido.getFechaHoraCreacion().substring(11, 16) : "?"; } catch (Exception ex) { horaStr = "??:??"; }
                 }
             }
             return new javafx.beans.property.SimpleStringProperty(horaStr);
         });
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        estadoPedidoCol.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Formatear columna Total como moneda (directamente)
         totalCol.setCellFactory(tc -> new TableCell<PedidoMesaDTO, Double>() {
             @Override
             protected void updateItem(Double price, boolean empty) {
                 super.updateItem(price, empty);
                 setText(empty || price == null ? null : CURRENCY_FORMATTER.format(price));
                 // Opcional: Alinear a la derecha
                 setStyle("-fx-alignment: CENTER-RIGHT;");
             }
         });

        pedidosListosTable.setItems(pedidosListData);

        // Tabla Derecha (Detalle Pedido)
        detalleProductoCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        detalleCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        detallePrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        detalleSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        detallePrecioCol.setCellFactory(col -> new TableCell<DetallePedidoMesaDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMATTER.format(price));
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        // Para columna Subtotal
        detalleSubtotalCol.setCellFactory(col -> new TableCell<DetallePedidoMesaDTO, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                if (empty || subtotal == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMATTER.format(subtotal));
                }
                 // Opcional: Alinear a la derecha
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
        // --- FIN CELL FACTORY DIRECTO ---

        detallePedidoTable.setItems(detallePedidoData);
    }

    private void cargarPedidosListos() {
        pedidosListosTable.setPlaceholder(new Label("Cargando pedidos..."));
        statusLabelCashier.setText("Actualizando lista de pedidos...");
        statusLabelCashier.getStyleClass().setAll("lbl-warning");

        // MODIFICADO: Uso de ThreadManager
        ThreadManager.getInstance().execute(() -> {
            try {
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                
                // El cajero SOLO debe ver los pedidos listos para cobrar.
                List<PedidoMesaDTO> pedidosParaCaja = todosLosPedidos.stream()
                        .filter(p -> "LISTO_PARA_ENTREGAR".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    pedidosListData.clear();
                    if (pedidosParaCaja != null) { pedidosListData.addAll(pedidosParaCaja); }
                    pedidosListosTable.setPlaceholder(new Label(pedidosListData.isEmpty() ? "No hay pedidos pendientes de cobro." : "Error al cargar."));
                    statusLabelCashier.setText("Lista actualizada. " + pedidosListData.size() + " pedidos pendientes.");
                    statusLabelCashier.getStyleClass().setAll("lbl-success");
                    limpiarDetalle();
                });

            } catch (HttpClientService.AuthenticationException e) { 
                Platform.runLater(() -> handleError("Error de autenticación.", e));
            } catch (IOException | InterruptedException e) { 
                Platform.runLater(() -> handleError("Error de red.", e));
            } catch (Exception e) { 
                Platform.runLater(() -> handleError("Error inesperado.", e)); 
            }
        });
    }

    private void mostrarDetallePedido(PedidoMesaDTO pedido) {
        this.pedidoSeleccionado = pedido;

        if (pedido == null) {
            limpiarDetalle();
        } else {
            detallePedidoPane.setDisable(false);
            pedidoSeleccionadoLabel.setText("Detalle Pedido - Mesa: " + pedido.getNumeroMesa());
            detallePedidoData.clear();
            if (pedido.getDetalles() != null) { detallePedidoData.addAll(pedido.getDetalles()); }

            double subtotal = 0;
            if(pedido.getTotal() != null) { 
                // Usamos el total del backend y recalculamos el subtotal base
                subtotal = pedido.getTotal() / 1.18; 
            }
            // Si el total es 0 o nulo, calcularlo desde los detalles
            if (subtotal == 0) {
                 subtotal = detallePedidoData.stream().mapToDouble(DetallePedidoMesaDTO::getSubtotal).sum();
            }
            
            double igv = subtotal * 0.18;
            double total = subtotal + igv;

            subTotalDetalleLabel.setText(CURRENCY_FORMATTER.format(subtotal));
            igvDetalleLabel.setText(CURRENCY_FORMATTER.format(igv));
            totalDetalleLabel.setText(CURRENCY_FORMATTER.format(total));

            cerrarPedidoButton.setDisable(false);
            statusLabelCashier.setText("Mostrando detalles para Mesa " + pedido.getNumeroMesa());
            statusLabelCashier.getStyleClass().setAll("lbl-info");
        }
    }

    private void limpiarDetalle() {
        pedidoSeleccionado = null;
        detallePedidoPane.setDisable(true);
        pedidoSeleccionadoLabel.setText("Detalle Pedido: (Seleccione un pedido)");
        detallePedidoData.clear();
        subTotalDetalleLabel.setText("S/ 0.00");
        igvDetalleLabel.setText("S/ 0.00");
        totalDetalleLabel.setText("S/ 0.00");
        cerrarPedidoButton.setDisable(true);
    }

    @FXML
    private void handleCerrarPedido() {
        if (pedidoSeleccionado == null) {
            handleError("No hay pedido seleccionado.", null);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Cierre");
        confirmacion.setHeaderText("Cerrar Pedido - Mesa " + pedidoSeleccionado.getNumeroMesa());
        confirmacion.setContentText("¿Cerrar este pedido?\nTotal: " + totalDetalleLabel.getText());
        Optional<ButtonType> resultado = confirmacion.showAndWait();

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            statusLabelCashier.setText("Cerrando pedido...");
            statusLabelCashier.getStyleClass().setAll("lbl-warning");
            cerrarPedidoButton.setDisable(true);
            Long pedidoIdParaCerrar = pedidoSeleccionado.getIdPedidoMesa();
            ThreadManager.getInstance().execute(() -> {
                try {
                    pedidoMesaService.cerrarPedido(pedidoIdParaCerrar);
                    Platform.runLater(() -> {
                        statusLabelCashier.setText("Pedido Mesa " + pedidoSeleccionado.getNumeroMesa() + " cerrado.");
                        statusLabelCashier.getStyleClass().setAll("lbl-success");
                        limpiarDetalle();
                        cargarPedidosListos();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> { 
                        handleError("Error al cerrar pedido: " + e.getMessage(), e); 
                        cerrarPedidoButton.setDisable(false); 
                    });
                }
            });
        }
    }

    private void handleError(String message, Exception e) {
        System.err.println(message);
        if (e != null) { e.printStackTrace(); }
        statusLabelCashier.setText(message);
        statusLabelCashier.getStyleClass().setAll("lbl-danger");
    }
}