package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform; // Importado en paso anterior
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML; // Importado en paso anterior
import javafx.geometry.Insets; // Importado en paso anterior
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // Importado en paso anterior
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;

// --- ¡¡IMPLEMENTAR INTERFAZ!! ---
public class KitchenController implements CleanableController {
// --- FIN IMPLEMENTACIÓN ---

    @FXML private TilePane pedidosContainer;
    @FXML private Label statusLabelKitchen;

    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();
    private Timeline refreshTimeline;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        statusLabelKitchen.setText("Cargando pedidos pendientes...");
        statusLabelKitchen.getStyleClass().setAll("lbl-info");
        pedidosContainer.getChildren().clear();

        cargarPedidosPendientes();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(30), event -> {
                System.out.println("Actualizando pedidos de cocina...");
                cargarPedidosPendientes();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    // --- ¡¡MÉTODO MODIFICADO!! ---
    private void cargarPedidosPendientes() {
        statusLabelKitchen.setText("Actualizando...");
        statusLabelKitchen.getStyleClass().setAll("lbl-warning");

        new Thread(() -> {
            try {
                // Idealmente filtrar en backend: ?estado=ABIERTO
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                
                // --- ¡¡MODIFICACIÓN CLAVE!! ---
                // Filtramos pedidos ABIERTOS (enviados por mesero)
                // Y que además tengan AL MENOS UN item en estado "PENDIENTE"
                List<PedidoMesaDTO> pedidosPendientes = todosLosPedidos.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()))
                        .filter(p -> p.getDetalles() != null && p.getDetalles().stream()
                                .anyMatch(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle())))
                        .collect(Collectors.toList());
                // --- FIN MODIFICACIÓN ---

                Platform.runLater(() -> {
                    mostrarPedidosEnUI(pedidosPendientes);
                    statusLabelKitchen.setText("Pedidos actualizados. Mostrando " + pedidosPendientes.size() + " comandas pendientes.");
                    statusLabelKitchen.getStyleClass().setAll("lbl-success");
                });

            } catch (HttpClientService.AuthenticationException e) {
                 Platform.runLater(() -> handleError("Error de autenticación al cargar pedidos.", e));
            } catch (IOException | InterruptedException e) {
                 Platform.runLater(() -> handleError("Error de red al cargar pedidos.", e));
            } catch (Exception e) {
                 Platform.runLater(() -> handleError("Error inesperado al cargar pedidos.", e));
            }
        }).start();
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

    // --- ¡¡MÉTODO MODIFICADO!! ---
    private VBox crearTarjetaPedido(PedidoMesaDTO pedido) {
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(10));
        tarjeta.getStyleClass().add("card");
        tarjeta.setPrefWidth(250);
        tarjeta.setMinWidth(250);

        Label titulo = new Label("Mesa " + pedido.getNumeroMesa());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14pt;");

        String horaFormateada = pedido.getFechaHoraCreacion();
        try {
            LocalDateTime fechaHora = LocalDateTime.parse(pedido.getFechaHoraCreacion());
            horaFormateada = fechaHora.format(TIME_FORMATTER);
        } catch (Exception e) { /* Ignorar error de formato */ }
        Label horaLabel = new Label("Hora: " + horaFormateada);
        horaLabel.setStyle("-fx-font-size: 10pt; -fx-text-fill: #9ca3af;");

        ListView<String> productosList = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        
        // --- ¡¡MODIFICACIÓN CLAVE!! ---
        // Mostramos SOLAMENTE los items que están PENDIENTES
        if (pedido.getDetalles() != null) { 
            List<DetallePedidoMesaDTO> detallesPendientes = pedido.getDetalles().stream()
                    .filter(d -> "PENDIENTE".equalsIgnoreCase(d.getEstadoDetalle()))
                    .collect(Collectors.toList());

            for (DetallePedidoMesaDTO detalle : detallesPendientes) {
                items.add(detalle.getCantidad() + " x " + detalle.getNombreProducto());
            }
        }
        // --- FIN MODIFICACIÓN ---
        
        productosList.setItems(items);
        productosList.setPrefHeight(100);
        productosList.setFocusTraversable(false);
        productosList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        Button btnListo = new Button("Marcar Listo");
        btnListo.getStyleClass().addAll("btn-success");
        btnListo.setMaxWidth(Double.MAX_VALUE);
        btnListo.setOnAction(event -> handleMarcarListo(pedido, tarjeta));

        tarjeta.getChildren().addAll(titulo, horaLabel, productosList, btnListo);

        return tarjeta;
    }

    // --- ¡¡MÉTODO MODIFICADO!! ---
    private void handleMarcarListo(PedidoMesaDTO pedido, Node tarjetaNode) {
        tarjetaNode.setOpacity(0.5);
        tarjetaNode.setDisable(true);
        statusLabelKitchen.setText("Marcando comanda Mesa " + pedido.getNumeroMesa() + " como lista...");
        statusLabelKitchen.getStyleClass().setAll("lbl-warning");

        // Ya no usamos NUEVO_ESTADO, llamamos al endpoint específico
        // final String NUEVO_ESTADO = "LISTO_PARA_ENTREGAR"; 

        new Thread(() -> {
            try {
                // --- ¡¡MODIFICACIÓN CLAVE!! ---
                // Llamamos al nuevo endpoint que marca solo los PENDIENTES como LISTO
                PedidoMesaDTO pedidoActualizado = pedidoMesaService.marcarPendientesComoListos(pedido.getIdPedidoMesa());
                // --- FIN MODIFICACIÓN ---

                Platform.runLater(() -> {
                    // La lógica de respuesta sigue siendo válida:
                    // El backend pondrá el pedido como LISTO_PARA_ENTREGAR si marcó items.
                    if (pedidoActualizado != null && "LISTO_PARA_ENTREGAR".equalsIgnoreCase(pedidoActualizado.getEstado())) {
                        statusLabelKitchen.setText("Comanda Mesa " + pedido.getNumeroMesa() + " marcada como lista.");
                        statusLabelKitchen.getStyleClass().setAll("lbl-success");
                        pedidosContainer.getChildren().remove(tarjetaNode); // Quitamos la tarjeta
                        if (pedidosContainer.getChildren().isEmpty()) {
                            pedidosContainer.getChildren().add(new Label("No hay pedidos pendientes."));
                        }
                    } else {
                        // Esto podría pasar si hubo un error o si el mesero añadió
                        // más items justo ahora, volviendo el pedido a ABIERTO.
                        handleError("Error: El estado del pedido no se actualizó (quizás fue modificado).", null);
                        tarjetaNode.setOpacity(1.0);
                        tarjetaNode.setDisable(false);
                        cargarPedidosPendientes(); // Recargar por si acaso
                    }
                });

            } catch (HttpClientService.AuthenticationException e) {
                 Platform.runLater(() -> { handleError("Error de permisos.", e); tarjetaNode.setOpacity(1.0); tarjetaNode.setDisable(false); });
            } catch (IOException | InterruptedException e) {
                 Platform.runLater(() -> { handleError("Error de red.", e); tarjetaNode.setOpacity(1.0); tarjetaNode.setDisable(false); });
            } catch (Exception e) {
                 Platform.runLater(() -> { handleError("Error al marcar pedido: " + e.getMessage(), e); tarjetaNode.setOpacity(1.0); tarjetaNode.setDisable(false); });
            }
        }).start();
    }


    private void handleError(String message, Exception e) {
        System.err.println(message);
        if (e != null) { e.printStackTrace(); }
        statusLabelKitchen.setText(message);
        statusLabelKitchen.getStyleClass().setAll("lbl-danger");
    }

    /**
     * Método de la interfaz CleanableController. Detiene el Timeline.
     */
    @Override // <-- AÑADIR @Override
    public void cleanup() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            System.out.println("Timeline de cocina detenido.");
        }
    }
}