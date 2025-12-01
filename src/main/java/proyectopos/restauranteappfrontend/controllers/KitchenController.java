package proyectopos.restauranteappfrontend.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoWebDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoWebDTO;
import proyectopos.restauranteappfrontend.model.dto.WebSocketMessageDTO;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.PedidoWebService;
import proyectopos.restauranteappfrontend.services.WebSocketService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class KitchenController implements CleanableController {

    @FXML private TilePane pedidosContainer;    // Contenedor Mesas
    @FXML private TilePane deliveryContainer;   // Contenedor Delivery/Web
    @FXML private Label statusLabelKitchen;

    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    private final PedidoWebService pedidoWebService = new PedidoWebService();
    private final HttpClientService httpClient = new HttpClientService(); 
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        statusLabelKitchen.setText("Cargando sistema de cocina...");
        pedidosContainer.getChildren().clear();
        deliveryContainer.getChildren().clear();

        // 1. Cargar datos iniciales
        cargarTodosLosPedidos();

        // 2. Suscripción a WebSockets
        WebSocketService.getInstance().subscribe("/topic/pedidos", (jsonMessage) -> {
            Platform.runLater(() -> procesarMensajeWebSocket(jsonMessage));
        });
    }

    private void procesarMensajeWebSocket(String jsonMessage) {
        try {
            System.out.println("COCINA WS RECIBIDO: " + jsonMessage); // DEBUG

            WebSocketMessageDTO msg = gson.fromJson(jsonMessage, WebSocketMessageDTO.class);
            if (msg == null || msg.getType() == null) return;

            switch (msg.getType()) {
                // --- EVENTOS DE MESAS ---
                case "PEDIDO_CREADO":
                case "PEDIDO_ACTUALIZADO":
                    PedidoMesaDTO pedidoMesa = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
                    if (pedidoMesa != null) actualizarTarjetaMesa(pedidoMesa);
                    break;
                
                // AQUÍ ESTABA EL PROBLEMA:
                case "PEDIDO_LISTO":
                case "PEDIDO_CERRADO":
                case "PEDIDO_CANCELADO":
                    PedidoMesaDTO pedidoCerrado = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
                    if (pedidoCerrado != null) {
                        System.out.println("Intentando remover tarjeta ID: " + pedidoCerrado.getIdPedidoMesa());
                        removerTarjetaMesa(pedidoCerrado.getIdPedidoMesa());
                    }
                    break;

                // --- EVENTOS WEB / DELIVERY ---
                case "NUEVO_PEDIDO_WEB":
                    PedidoWebDTO pedidoWeb = gson.fromJson(msg.getPayload(), PedidoWebDTO.class);
                    if (pedidoWeb != null) {
                        agregarTarjetaWeb(pedidoWeb);
                        reproducirSonidoAlerta();
                    }
                    break;
                
                case "PEDIDO_WEB_ACTUALIZADO":
                    PedidoWebDTO webActualizado = gson.fromJson(msg.getPayload(), PedidoWebDTO.class);
                    if (webActualizado != null) {
                        if (!"PENDIENTE".equals(webActualizado.getEstado()) && !"EN_COCINA".equals(webActualizado.getEstado())) {
                             removerTarjetaWeb(webActualizado.getIdPedidoWeb());
                        }
                    }
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error WS en Cocina: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- UTILIDADES ---

    // CORRECCIÓN 1: Comparar IDs como String para evitar problemas Long vs Integer
    private void removerTarjetaMesa(Long id) {
        if (id == null) return;
        String idStr = String.valueOf(id);
        
        boolean eliminado = pedidosContainer.getChildren().removeIf(n -> {
            if (n.getUserData() == null) return false;
            return n.getUserData().toString().equals(idStr);
        });

        if (eliminado) System.out.println("Tarjeta mesa " + id + " eliminada correctamente.");

        if (pedidosContainer.getChildren().isEmpty()) {
            pedidosContainer.getChildren().add(new Label("Sin comandas de salón."));
        }
    }

    // CORRECCIÓN 2: Lo mismo para Web
    private void removerTarjetaWeb(Long id) {
        if (id == null) return;
        String idStr = String.valueOf(id);
        deliveryContainer.getChildren().removeIf(n -> n.getUserData() != null && n.getUserData().toString().equals(idStr));
        if (deliveryContainer.getChildren().isEmpty()) deliveryContainer.getChildren().add(new Label("Sin pedidos web."));
    }

    // CORRECCIÓN 3: Reemplazar también usando String
    private void reemplazarOAgregar(TilePane container, Long id, Node nodoNuevo) {
        Node existente = null;
        String idStr = String.valueOf(id);

        for (Node n : container.getChildren()) {
            if (n.getUserData() != null && n.getUserData().toString().equals(idStr)) {
                existente = n;
                break;
            }
        }
        
        container.getChildren().removeIf(n -> n instanceof Label);

        if (existente != null) {
            int idx = container.getChildren().indexOf(existente);
            container.getChildren().set(idx, nodoNuevo);
        } else {
            container.getChildren().add(nodoNuevo);
        }
    }

    // --- RESTO DE LÓGICA ---

    private void cargarTodosLosPedidos() {
        ThreadManager.getInstance().execute(() -> {
            try {
                List<PedidoMesaDTO> mesas = pedidoMesaService.getAllPedidos();
                List<PedidoMesaDTO> pendientesMesa = mesas.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()))
                        .filter(p -> p.getDetalles().stream().anyMatch(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle())))
                        .collect(Collectors.toList());

                List<PedidoWebDTO> pendientesWeb = pedidoWebService.getPedidosWebActivos();

                Platform.runLater(() -> {
                    mostrarMesasEnUI(pendientesMesa);
                    mostrarWebEnUI(pendientesWeb);
                    statusLabelKitchen.setText("Actualizado: " + pendientesMesa.size() + " mesas, " + pendientesWeb.size() + " web.");
                    statusLabelKitchen.getStyleClass().setAll("lbl-success");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabelKitchen.setText("Conexión: " + e.getMessage());
                    statusLabelKitchen.getStyleClass().setAll("lbl-danger");
                });
            }
        });
    }

    private void mostrarMesasEnUI(List<PedidoMesaDTO> pedidos) {
        pedidosContainer.getChildren().clear();
        if (pedidos.isEmpty()) pedidosContainer.getChildren().add(new Label("Sin comandas de salón."));
        pedidos.forEach(this::actualizarTarjetaMesa);
    }

    private void actualizarTarjetaMesa(PedidoMesaDTO pedido) {
        boolean tienePendientes = pedido.getDetalles().stream().anyMatch(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle()));
        if (!tienePendientes) {
            removerTarjetaMesa(pedido.getIdPedidoMesa());
            return;
        }
        Node tarjetaNueva = crearTarjetaMesaUI(pedido);
        reemplazarOAgregar(pedidosContainer, pedido.getIdPedidoMesa(), tarjetaNueva);
    }
    
    private void mostrarWebEnUI(List<PedidoWebDTO> pedidos) {
        deliveryContainer.getChildren().clear();
        if (pedidos.isEmpty()) deliveryContainer.getChildren().add(new Label("Sin pedidos web."));
        pedidos.forEach(this::agregarTarjetaWeb);
    }

    private void agregarTarjetaWeb(PedidoWebDTO pedido) {
        Node tarjeta = crearTarjetaWebUI(pedido);
        reemplazarOAgregar(deliveryContainer, pedido.getIdPedidoWeb(), tarjeta);
    }

    private Node crearTarjetaMesaUI(PedidoMesaDTO pedido) {
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(10));
        tarjeta.getStyleClass().add("kitchen-command-card");
        tarjeta.setPrefWidth(250);
        tarjeta.setUserData(pedido.getIdPedidoMesa()); // ID guardado aquí

        Label titulo = new Label("Mesa " + pedido.getNumeroMesa());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14pt;");
        
        ListView<String> lista = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        pedido.getDetalles().stream()
              .filter(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle()))
              .forEach(d -> items.add(d.getCantidad() + " x " + d.getNombreProducto()));
        lista.setItems(items);
        lista.setPrefHeight(150);

        Button btnListo = new Button("Mesa Lista");
        btnListo.getStyleClass().addAll("btn-success");
        btnListo.setMaxWidth(Double.MAX_VALUE);
        btnListo.setOnAction(e -> handleMarcarMesaLista(pedido, btnListo));

        tarjeta.getChildren().addAll(titulo, new Label(formatearHora(pedido.getFechaHoraCreacion())), lista, btnListo);
        return tarjeta;
    }

    private Node crearTarjetaWebUI(PedidoWebDTO pedido) {
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(10));
        tarjeta.getStyleClass().add("card"); 
        tarjeta.setStyle("-fx-background-color: #e0f2fe; -fx-border-color: #0284c7;");
        tarjeta.setPrefWidth(250);
        tarjeta.setUserData(pedido.getIdPedidoWeb());

        Label titulo = new Label("Delivery #" + pedido.getIdPedidoWeb());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14pt; -fx-text-fill: #0369a1;");
        
        Label cliente = new Label(pedido.getNombreCliente());
        cliente.setStyle("-fx-font-weight: bold;");
        
        Label direccion = new Label("📍 " + pedido.getDireccionEntrega());
        direccion.setWrapText(true);

        ListView<String> lista = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        if (pedido.getDetalles() != null) {
            for (DetallePedidoWebDTO det : pedido.getDetalles()) {
                String item = det.getCantidad() + " x " + det.getNombreProducto();
                if (det.getObservaciones() != null && !det.getObservaciones().isEmpty()) {
                    item += "\n (" + det.getObservaciones() + ")";
                }
                items.add(item);
            }
        }
        lista.setItems(items);
        lista.setPrefHeight(150);

        Button btnDespachar = new Button("Despachar");
        btnDespachar.getStyleClass().addAll("btn-primary");
        btnDespachar.setMaxWidth(Double.MAX_VALUE);
        
        btnDespachar.setOnAction(e -> {
            btnDespachar.setDisable(true); 
            ThreadManager.getInstance().execute(() -> {
                try {
                    Map<String, String> body = Map.of("estado", "EN_CAMINO");
                    httpClient.put("/api/web/pedidos/" + pedido.getIdPedidoWeb() + "/estado", body, PedidoWebDTO.class);
                    Platform.runLater(() -> removerTarjetaWeb(pedido.getIdPedidoWeb()));
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnDespachar.setDisable(false);
                        statusLabelKitchen.setText("Error: " + ex.getMessage());
                    });
                }
            });
        });

        tarjeta.getChildren().addAll(titulo, cliente, direccion, lista, btnDespachar);
        return tarjeta;
    }

    private String formatearHora(String fechaIso) {
        try {
            return LocalDateTime.parse(fechaIso).format(TIME_FORMATTER);
        } catch (Exception e) { return ""; }
    }

    private void handleMarcarMesaLista(PedidoMesaDTO pedido, Node nodo) {
        nodo.setDisable(true);
        ThreadManager.getInstance().execute(() -> {
            try {
                pedidoMesaService.marcarPendientesComoListos(pedido.getIdPedidoMesa());
            } catch (Exception e) {
                Platform.runLater(() -> {
                    nodo.setDisable(false);
                    statusLabelKitchen.setText("Error al marcar listo: " + e.getMessage());
                });
            }
        });
    }

    private void reproducirSonidoAlerta() {
        // Toolkit.getDefaultToolkit().beep(); 
    }

    @Override public void cleanup() { }
}