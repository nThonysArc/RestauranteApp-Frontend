package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson; // Necesario para deserializar el mensaje del WebSocket

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList; 
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button; 
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode; 
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.WebSocketMessageDTO; // Asegúrate de haber creado esta clase
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.WebSocketService;
import proyectopos.restauranteappfrontend.util.ThreadManager;


public class KitchenController implements CleanableController {

    @FXML private TilePane pedidosContainer;
    @FXML private Label statusLabelKitchen;

    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        statusLabelKitchen.setText("Cargando pedidos pendientes...");
        statusLabelKitchen.getStyleClass().setAll("lbl-info");
        pedidosContainer.getChildren().clear();

        // Carga inicial completa
        cargarPedidosPendientes();

        // Suscripción a WebSockets con manejo de objetos (Delta Updates)
        WebSocketService.getInstance().subscribe("/topic/pedidos", (jsonMessage) -> {
            Platform.runLater(() -> procesarMensajeWebSocket(jsonMessage));
        });
    }

    /**
     * Procesa el mensaje JSON recibido por WebSocket para actualizar la UI
     * sin necesidad de recargar toda la lista del servidor.
     */
    private void procesarMensajeWebSocket(String jsonMessage) {
        try {
            WebSocketMessageDTO msg = gson.fromJson(jsonMessage, WebSocketMessageDTO.class);
            
            if (msg == null || msg.getType() == null) return;

            // Eventos que añaden o actualizan un pedido (Nuevos items)
            if ("PEDIDO_CREADO".equals(msg.getType()) || "PEDIDO_ACTUALIZADO".equals(msg.getType())) {
                PedidoMesaDTO pedido = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
                if (pedido != null) {
                    System.out.println("WebSocket (Cocina): Pedido " + pedido.getIdPedidoMesa() + " recibido/actualizado.");
                    actualizarTarjetaPedido(pedido);
                }
            } 
            // Eventos que remueven el pedido de la vista de cocina (Ya está listo o cerrado)
            else if ("PEDIDO_LISTO".equals(msg.getType()) || 
                     "PEDIDO_CERRADO".equals(msg.getType()) || 
                     "PEDIDO_CANCELADO".equals(msg.getType())) {
                 
                 PedidoMesaDTO pedido = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
                 if (pedido != null) {
                     System.out.println("WebSocket (Cocina): Pedido " + pedido.getIdPedidoMesa() + " finalizado. Removiendo.");
                     removerTarjetaPedido(pedido.getIdPedidoMesa());
                 }
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje WebSocket: " + e.getMessage());
            e.printStackTrace();
            // Si falla la actualización parcial, hacemos fallback a la recarga completa
            cargarPedidosPendientes(); 
        }
    }

    /**
     * Busca si ya existe una tarjeta para el pedido. Si existe, la reemplaza.
     * Si no, la añade al final.
     */
    private void actualizarTarjetaPedido(PedidoMesaDTO pedido) {
        // Verificar si tiene items con estado PENDIENTE para cocina
        boolean tienePendientes = pedido.getDetalles() != null && pedido.getDetalles().stream()
                .anyMatch(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle()));

        // Si no tiene pendientes, no debería estar en esta pantalla
        if (!tienePendientes) {
            removerTarjetaPedido(pedido.getIdPedidoMesa());
            return;
        }

        // Buscar tarjeta existente por ID (guardado en userData)
        Node tarjetaExistente = null;
        for (Node node : pedidosContainer.getChildren()) {
            if (node.getUserData() instanceof Long && ((Long) node.getUserData()).equals(pedido.getIdPedidoMesa())) {
                tarjetaExistente = node;
                break;
            }
        }

        // Crear la nueva vista de la tarjeta
        Node nuevaTarjeta = crearTarjetaPedido(pedido);

        if (tarjetaExistente != null) {
            // Reemplazar en el mismo lugar
            int index = pedidosContainer.getChildren().indexOf(tarjetaExistente);
            pedidosContainer.getChildren().set(index, nuevaTarjeta);
        } else {
            // Añadir nueva
            pedidosContainer.getChildren().add(nuevaTarjeta);
            // Quitar mensaje de "No hay pedidos" si existe
            pedidosContainer.getChildren().removeIf(n -> n instanceof Label && ((Label)n).getText().startsWith("No hay pedidos"));
        }
        
        statusLabelKitchen.setText("Pedido Mesa " + pedido.getNumeroMesa() + " actualizado.");
        statusLabelKitchen.getStyleClass().setAll("lbl-success");
    }

    private void removerTarjetaPedido(Long idPedido) {
        boolean removido = pedidosContainer.getChildren().removeIf(node -> 
            node.getUserData() instanceof Long && ((Long) node.getUserData()).equals(idPedido)
        );
        
        if (removido && pedidosContainer.getChildren().isEmpty()) {
             pedidosContainer.getChildren().add(new Label("No hay pedidos pendientes en este momento."));
        }
    }

    // --- Carga inicial completa (Fallback) ---
    private void cargarPedidosPendientes() {
        statusLabelKitchen.setText("Sincronizando...");
        statusLabelKitchen.getStyleClass().setAll("lbl-warning");

        ThreadManager.getInstance().execute(() -> {
            try {
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                List<PedidoMesaDTO> pedidosPendientes = todosLosPedidos.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()))
                        .filter(p -> p.getDetalles() != null && p.getDetalles().stream()
                                .anyMatch(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle())))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    mostrarPedidosEnUI(pedidosPendientes);
                    statusLabelKitchen.setText("Sincronizado. " + pedidosPendientes.size() + " comandas pendientes.");
                    statusLabelKitchen.getStyleClass().setAll("lbl-success");
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

    private void mostrarPedidosEnUI(List<PedidoMesaDTO> pedidos) {
        pedidosContainer.getChildren().clear();

        if (pedidos == null || pedidos.isEmpty()) {
            pedidosContainer.getChildren().add(new Label("No hay pedidos pendientes en este momento."));
            return;
        }

        for (PedidoMesaDTO pedido : pedidos) {
            Node tarjetaPedido = crearTarjetaPedido(pedido);
            pedidosContainer.getChildren().add(tarjetaPedido);
        }
    }

    private VBox crearTarjetaPedido(PedidoMesaDTO pedido) {
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(10));
        // Clase CSS específica sugerida anteriormente, o usa 'card'
        tarjeta.getStyleClass().add("kitchen-command-card"); 
        if (!tarjeta.getStyleClass().contains("card")) tarjeta.getStyleClass().add("card"); // Fallback
        
        tarjeta.setPrefWidth(250);
        tarjeta.setMinWidth(250);
        
        // IMPORTANTE: Guardar el ID en el nodo para buscarlo después
        tarjeta.setUserData(pedido.getIdPedidoMesa());

        Label titulo = new Label("Mesa " + pedido.getNumeroMesa());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14pt;");

        String horaFormateada = pedido.getFechaHoraCreacion();
        try {
            LocalDateTime fechaHora = LocalDateTime.parse(pedido.getFechaHoraCreacion());
            horaFormateada = fechaHora.format(TIME_FORMATTER);
        } catch (Exception e) { /* Ignorar error */ }
        Label horaLabel = new Label("Hora: " + horaFormateada);
        horaLabel.setStyle("-fx-font-size: 10pt; -fx-text-fill: #9ca3af;");

        ListView<String> productosList = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        
        if (pedido.getDetalles() != null) { 
            List<DetallePedidoMesaDTO> detallesPendientes = pedido.getDetalles().stream()
                    .filter(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle()))
                    .collect(Collectors.toList());

            for (DetallePedidoMesaDTO detalle : detallesPendientes) {
                items.add(detalle.getCantidad() + " x " + detalle.getNombreProducto());
            }
        }
        
        productosList.setItems(items);
        productosList.setPrefHeight(150); // Un poco más alto para ver mejor
        productosList.setFocusTraversable(false);
        productosList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button btnListo = new Button("Marcar Listo");
        btnListo.getStyleClass().addAll("btn-success");
        btnListo.setMaxWidth(Double.MAX_VALUE);
        btnListo.setOnAction(event -> handleMarcarListo(pedido, tarjeta));

        tarjeta.getChildren().addAll(titulo, horaLabel, productosList, btnListo);

        return tarjeta;
    }

    private void handleMarcarListo(PedidoMesaDTO pedido, Node tarjetaNode) {
        tarjetaNode.setOpacity(0.5);
        tarjetaNode.setDisable(true);
        statusLabelKitchen.setText("Procesando Mesa " + pedido.getNumeroMesa() + "...");
        statusLabelKitchen.getStyleClass().setAll("lbl-warning");

        ThreadManager.getInstance().execute(() -> {
            try {
                // Esta llamada al backend actualizará la BD y enviará el WS "PEDIDO_LISTO"
                PedidoMesaDTO pedidoActualizado = pedidoMesaService.marcarPendientesComoListos(pedido.getIdPedidoMesa());
                
                Platform.runLater(() -> {
                    if (pedidoActualizado != null) {
                        statusLabelKitchen.setText("Mesa " + pedido.getNumeroMesa() + " lista.");
                        statusLabelKitchen.getStyleClass().setAll("lbl-success");
                        // No necesitamos remover manualmente aquí, el WebSocket recibirá "PEDIDO_LISTO"
                        // y llamará a removerTarjetaPedido, pero por reactividad inmediata lo hacemos:
                        removerTarjetaPedido(pedido.getIdPedidoMesa());
                    } else {
                        handleError("No se pudo actualizar el pedido.", null);
                        tarjetaNode.setOpacity(1.0);
                        tarjetaNode.setDisable(false);
                    }
                });

            } catch (Exception e) {
                 Platform.runLater(() -> { 
                     handleError("Error al marcar pedido: " + e.getMessage(), e); 
                     tarjetaNode.setOpacity(1.0); 
                     tarjetaNode.setDisable(false); 
                 });
            }
        });
    }

    private void handleError(String message, Exception e) {
        System.err.println(message);
        if (e != null) { e.printStackTrace(); }
        statusLabelKitchen.setText(message);
        statusLabelKitchen.getStyleClass().setAll("lbl-danger");
    }

    @Override 
    public void cleanup() {
        System.out.println("Limpiando KitchenController.");
        // Aquí se podrían desuscribir del tópico si WebSocketService soportara unsubscription por ID
    }
}