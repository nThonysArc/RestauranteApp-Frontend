package proyectopos.restauranteappfrontend.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map; // 1. Import necesario para Map.of
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
import proyectopos.restauranteappfrontend.services.HttpClientService; // 2. Import del servicio HTTP
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
    
    // 3. Instancia del cliente HTTP necesaria para las peticiones PUT
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
            WebSocketMessageDTO msg = gson.fromJson(jsonMessage, WebSocketMessageDTO.class);
            if (msg == null || msg.getType() == null) return;

            switch (msg.getType()) {
                // --- EVENTOS DE MESAS ---
                case "PEDIDO_CREADO":
                case "PEDIDO_ACTUALIZADO":
                    PedidoMesaDTO pedidoMesa = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
                    if (pedidoMesa != null) actualizarTarjetaMesa(pedidoMesa);
                    break;
                case "PEDIDO_LISTO":
                case "PEDIDO_CERRADO":
                case "PEDIDO_CANCELADO":
                    PedidoMesaDTO pedidoCerrado = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
                    if (pedidoCerrado != null) removerTarjetaMesa(pedidoCerrado.getIdPedidoMesa());
                    break;

                // --- EVENTOS WEB / DELIVERY ---
                case "NUEVO_PEDIDO_WEB":
                    PedidoWebDTO pedidoWeb = gson.fromJson(msg.getPayload(), PedidoWebDTO.class);
                    if (pedidoWeb != null) {
                        System.out.println("WebSocket: Nuevo pedido web ID " + pedidoWeb.getIdPedidoWeb());
                        agregarTarjetaWeb(pedidoWeb);
                        reproducirSonidoAlerta();
                    }
                    break;
                
                // Caso extra: Si otra pantalla actualiza el estado (Sincronización)
                case "PEDIDO_WEB_ACTUALIZADO":
                    PedidoWebDTO webActualizado = gson.fromJson(msg.getPayload(), PedidoWebDTO.class);
                    if (webActualizado != null) {
                        // Si ya no es PENDIENTE ni EN_COCINA, lo quitamos
                        if (!"PENDIENTE".equals(webActualizado.getEstado()) && !"EN_COCINA".equals(webActualizado.getEstado())) {
                             removerTarjetaWeb(webActualizado.getIdPedidoWeb());
                        }
                    }
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error WS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- LÓGICA DE CARGA INICIAL ---
    private void cargarTodosLosPedidos() {
        ThreadManager.getInstance().execute(() -> {
            try {
                // A. Cargar Pedidos de Mesa
                List<PedidoMesaDTO> mesas = pedidoMesaService.getAllPedidos();
                List<PedidoMesaDTO> pendientesMesa = mesas.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()))
                        .filter(p -> p.getDetalles().stream().anyMatch(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle())))
                        .collect(Collectors.toList());

                // B. Cargar Pedidos Web
                List<PedidoWebDTO> pendientesWeb = pedidoWebService.getPedidosWebActivos();

                Platform.runLater(() -> {
                    mostrarMesasEnUI(pendientesMesa);
                    mostrarWebEnUI(pendientesWeb);
                    statusLabelKitchen.setText("Actualizado: " + pendientesMesa.size() + " mesas, " + pendientesWeb.size() + " delivery.");
                    statusLabelKitchen.getStyleClass().setAll("lbl-success");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabelKitchen.setText("Error de conexión: " + e.getMessage());
                    statusLabelKitchen.getStyleClass().setAll("lbl-danger");
                });
            }
        });
    }

    // --- UI PARA PEDIDOS DE MESA ---
    private void mostrarMesasEnUI(List<PedidoMesaDTO> pedidos) {
        pedidosContainer.getChildren().clear();
        if (pedidos.isEmpty()) pedidosContainer.getChildren().add(new Label("Todavía no hay pedidos"));
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
    
    // --- UI PARA PEDIDOS WEB ---
    private void mostrarWebEnUI(List<PedidoWebDTO> pedidos) {
        deliveryContainer.getChildren().clear();
        if (pedidos.isEmpty()) deliveryContainer.getChildren().add(new Label("Sin pedidos web."));
        pedidos.forEach(this::agregarTarjetaWeb);
    }

    private void agregarTarjetaWeb(PedidoWebDTO pedido) {
        Node tarjeta = crearTarjetaWebUI(pedido);
        reemplazarOAgregar(deliveryContainer, pedido.getIdPedidoWeb(), tarjeta);
    }

    // --- CREACIÓN DE TARJETAS ---
    
    private Node crearTarjetaMesaUI(PedidoMesaDTO pedido) {
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(10));
        tarjeta.getStyleClass().add("kitchen-command-card");
        tarjeta.setPrefWidth(250);
        tarjeta.setUserData(pedido.getIdPedidoMesa());

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
        btnListo.setOnAction(e -> handleMarcarMesaLista(pedido, tarjeta));

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
        
        // --- LÓGICA CORREGIDA DEL BOTÓN ---
        btnDespachar.setOnAction(e -> {
            btnDespachar.setDisable(true); 

            ThreadManager.getInstance().execute(() -> {
                try {
                    // Creamos el body JSON para enviar al backend
                    Map<String, String> body = Map.of("estado", "EN_CAMINO");
                    
                    // Llamada PUT al backend usando httpClient
                    httpClient.put("/api/web/pedidos/" + pedido.getIdPedidoWeb() + "/estado", body, PedidoWebDTO.class);
                    
                    // Actualizamos la UI inmediatamente
                    Platform.runLater(() -> {
                        removerTarjetaWeb(pedido.getIdPedidoWeb());
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        btnDespachar.setDisable(false);
                        statusLabelKitchen.setText("Error al despachar: " + ex.getMessage());
                        statusLabelKitchen.getStyleClass().setAll("lbl-danger");
                    });
                }
            });
        });

        tarjeta.getChildren().addAll(titulo, cliente, direccion, lista, btnDespachar);
        return tarjeta;
    }

    // --- UTILIDADES ---

    private void reemplazarOAgregar(TilePane container, Long id, Node nodoNuevo) {
        Node existente = null;
        for (Node n : container.getChildren()) {
            if (n.getUserData() instanceof Long && ((Long)n.getUserData()).equals(id)) {
                existente = n;
                break;
            }
        }
        
        container.getChildren().removeIf(n -> n instanceof Label && ((Label)n).getText().startsWith("Sin "));

        if (existente != null) {
            int idx = container.getChildren().indexOf(existente);
            container.getChildren().set(idx, nodoNuevo);
        } else {
            container.getChildren().add(nodoNuevo);
        }
    }

    private void removerTarjetaMesa(Long id) {
        pedidosContainer.getChildren().removeIf(n -> n.getUserData() instanceof Long && ((Long)n.getUserData()).equals(id));
        if (pedidosContainer.getChildren().isEmpty()) pedidosContainer.getChildren().add(new Label("Sin comandas de salón."));
    }
    
    // Método auxiliar para remover tarjetas web (usado por el botón y el WebSocket)
    private void removerTarjetaWeb(Long id) {
        deliveryContainer.getChildren().removeIf(n -> n.getUserData() instanceof Long && ((Long)n.getUserData()).equals(id));
        if (deliveryContainer.getChildren().isEmpty()) deliveryContainer.getChildren().add(new Label("Sin pedidos web."));
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
                Platform.runLater(() -> nodo.setDisable(false));
            }
        });
    }

    private void reproducirSonidoAlerta() {
        // Toolkit.getDefaultToolkit().beep(); 
    }

    @Override public void cleanup() { }
}