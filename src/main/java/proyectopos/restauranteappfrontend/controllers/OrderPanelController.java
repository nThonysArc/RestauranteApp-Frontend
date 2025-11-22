package proyectopos.restauranteappfrontend.controllers;

import java.util.ArrayList;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class OrderPanelController {

    @FXML private TableView<DetallePedidoMesaDTO> pedidoActualTableView;
    @FXML private TableColumn<DetallePedidoMesaDTO, String> pedidoNombreCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Integer> pedidoCantidadCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoPrecioCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoSubtotalCol;
    
    @FXML private Label subTotalPedidoLabel;
    @FXML private Label igvPedidoLabel;
    @FXML private Label totalPedidoLabel;
    @FXML private Button crearPedidoButton;

    // Datos
    private final ObservableList<DetallePedidoMesaDTO> itemsCompletosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> itemsEnviadosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> itemsNuevosData = FXCollections.observableArrayList();
    
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    
    // Estado actual
    private MesaDTO mesaActual;
    private PedidoMesaDTO pedidoActual;
    private Runnable onPedidoEnviadoCallback; // Para avisar al Dashboard que refresque si es necesario

    @FXML
    public void initialize() {
        configurarTabla();
        crearPedidoButton.setDisable(true);
    }

    private void configurarTabla() {
        pedidoNombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        pedidoCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        pedidoPrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        pedidoSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        pedidoActualTableView.setItems(itemsCompletosData);
    }

    // --- API Pública para el DashboardController ---

    public void setMesa(MesaDTO mesa, PedidoMesaDTO pedidoExistente) {
        this.mesaActual = mesa;
        this.pedidoActual = pedidoExistente;
        
        limpiarPanel();
        
        if (pedidoExistente != null) {
            if (pedidoExistente.getDetalles() != null) {
                itemsEnviadosData.addAll(pedidoExistente.getDetalles());
            }
            crearPedidoButton.setText("Añadir al Pedido");
        } else {
            crearPedidoButton.setText("Enviar a Cocina");
        }
        
        actualizarVista();
    }
    
    public void setOnPedidoEnviado(Runnable callback) {
        this.onPedidoEnviadoCallback = callback;
    }

    public void agregarProducto(ProductoDTO producto, int cantidad) {
        if (cantidad <= 0) return;

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
        actualizarVista();
    }

    // --- Lógica Interna ---

    private void limpiarPanel() {
        itemsEnviadosData.clear();
        itemsNuevosData.clear();
        itemsCompletosData.clear();
        actualizarTotales();
    }

    private void actualizarVista() {
        itemsCompletosData.clear();
        itemsCompletosData.addAll(itemsEnviadosData);
        itemsCompletosData.addAll(itemsNuevosData);
        actualizarTotales();
        
        boolean deshabilitar = (mesaActual == null || itemsNuevosData.isEmpty());
        crearPedidoButton.setDisable(deshabilitar);
        pedidoActualTableView.refresh();
    }

    private void actualizarTotales() {
        double subtotal = 0.0;
        for (DetallePedidoMesaDTO detalle : itemsCompletosData) {
            // Aseguramos que el subtotal esté calculado
            detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
            subtotal += detalle.getSubtotal();
        }
        double igv = subtotal * 0.18;
        double total = subtotal + igv;

        subTotalPedidoLabel.setText(String.format("S/ %.2f", subtotal));
        igvPedidoLabel.setText(String.format("S/ %.2f", igv));
        totalPedidoLabel.setText(String.format("S/ %.2f", total));
    }

    @FXML
    private void handleEnviarPedido() {
        if (mesaActual == null || itemsNuevosData.isEmpty()) return;

        crearPedidoButton.setDisable(true); // Evitar doble clic

        PedidoMesaDTO pedidoDTO = new PedidoMesaDTO();
        pedidoDTO.setIdMesa(mesaActual.getIdMesa());
        pedidoDTO.setEstado("ABIERTO");
        pedidoDTO.setDetalles(new ArrayList<>(itemsNuevosData));

        ThreadManager.getInstance().execute(() -> {
            try {
                if (this.pedidoActual == null) {
                    // Crear nuevo
                    pedidoMesaService.crearPedido(pedidoDTO);
                } else {
                    // Actualizar existente
                    pedidoMesaService.actualizarPedido(this.pedidoActual.getIdPedidoMesa(), pedidoDTO);
                }

                Platform.runLater(() -> {
                    // Notificar al padre
                    if (onPedidoEnviadoCallback != null) onPedidoEnviadoCallback.run();
                    
                    // Resetear estado local para la próxima
                    itemsEnviadosData.addAll(itemsNuevosData); // Pasarlos a "enviados" visualmente
                    itemsNuevosData.clear();
                    actualizarVista();
                    
                    // Opcional: Mostrar alerta pequeña o toast
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR, "Error al enviar pedido: " + e.getMessage()).show();
                    crearPedidoButton.setDisable(false);
                });
            }
        });
    }
}