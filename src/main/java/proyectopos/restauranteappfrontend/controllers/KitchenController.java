package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button; // <-- IMPORTACIÓN AÑADIDA
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;

public class KitchenController {

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

    private void cargarPedidosPendientes() {
        statusLabelKitchen.setText("Actualizando...");
        statusLabelKitchen.getStyleClass().setAll("lbl-warning");

        new Thread(() -> {
            try {
                List<PedidoMesaDTO> todosLosPedidos = pedidoMesaService.getAllPedidos();
                List<PedidoMesaDTO> pedidosPendientes = todosLosPedidos.stream()
                        .filter(p -> "ABIERTO".equalsIgnoreCase(p.getEstado()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    mostrarPedidosEnUI(pedidosPendientes);
                    statusLabelKitchen.setText("Pedidos actualizados. Mostrando " + pedidosPendientes.size() + " pendientes.");
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
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo formatear la fecha/hora del pedido " + pedido.getIdPedidoMesa());
        }
        Label horaLabel = new Label("Hora: " + horaFormateada);
        horaLabel.setStyle("-fx-font-size: 10pt; -fx-text-fill: #9ca3af;");

        ListView<String> productosList = new ListView<>();
        // Esta línea ahora debería funcionar con la importación correcta
        ObservableList<String> items = FXCollections.observableArrayList();
        for (DetallePedidoMesaDTO detalle : pedido.getDetalles()) {
            items.add(detalle.getCantidad() + " x " + detalle.getNombreProducto());
        }
        productosList.setItems(items);
        productosList.setPrefHeight(100);
        productosList.setFocusTraversable(false);
        // Esta línea ahora debería funcionar con la importación correcta
        productosList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        Button btnListo = new Button("Marcar Listo");
        btnListo.getStyleClass().addAll("btn-success");
        btnListo.setMaxWidth(Double.MAX_VALUE);
        btnListo.setOnAction(event -> handleMarcarListo(pedido, tarjeta));

        tarjeta.getChildren().addAll(titulo, horaLabel, productosList, btnListo);

        return tarjeta;
    }

// --- MÉTODO MODIFICADO: handleMarcarListo ---
    private void handleMarcarListo(PedidoMesaDTO pedido, Node tarjetaNode) {
        tarjetaNode.setOpacity(0.5);
        tarjetaNode.setDisable(true);
        statusLabelKitchen.setText("Marcando pedido Mesa " + pedido.getNumeroMesa() + " como listo...");
        statusLabelKitchen.getStyleClass().setAll("lbl-warning");

        // --- Definir el nuevo estado ---
        // Puedes cambiarlo a "EN_COCINA" si implementaste ese paso intermedio
        final String NUEVO_ESTADO = "LISTO_PARA_ENTREGAR";

        new Thread(() -> {
            try {
                // --- ¡¡LLAMADA REAL AL SERVICIO!! ---
                System.out.println("Llamando al backend para cambiar estado del pedido ID: "
                                    + pedido.getIdPedidoMesa() + " a " + NUEVO_ESTADO);
                PedidoMesaDTO pedidoActualizado = pedidoMesaService.cambiarEstadoPedido(pedido.getIdPedidoMesa(), NUEVO_ESTADO);
                // --- FIN LLAMADA REAL ---

                Platform.runLater(() -> {
                    // Verificar si el estado realmente cambió (el backend devuelve el DTO actualizado)
                    if (pedidoActualizado != null && NUEVO_ESTADO.equalsIgnoreCase(pedidoActualizado.getEstado())) {
                        statusLabelKitchen.setText("Pedido Mesa " + pedido.getNumeroMesa() + " marcado como listo.");
                        statusLabelKitchen.getStyleClass().setAll("lbl-success");
                        // Quitar la tarjeta de la vista
                        pedidosContainer.getChildren().remove(tarjetaNode);
                        if (pedidosContainer.getChildren().isEmpty()) {
                            pedidosContainer.getChildren().add(new Label("No hay pedidos pendientes."));
                        }
                    } else {
                        // Si el estado no cambió como se esperaba (raro, pero posible)
                        handleError("Error: El estado del pedido no se actualizó correctamente en el backend.", null);
                        tarjetaNode.setOpacity(1.0);
                        tarjetaNode.setDisable(false);
                    }
                });

            // Capturar excepciones específicas del servicio
            } catch (HttpClientService.AuthenticationException e) {
                 Platform.runLater(() -> {
                    handleError("Error de permisos al marcar pedido.", e);
                    tarjetaNode.setOpacity(1.0);
                    tarjetaNode.setDisable(false);
                 });
            } catch (IOException | InterruptedException e) {
                 Platform.runLater(() -> {
                    handleError("Error de red al marcar pedido.", e);
                    tarjetaNode.setOpacity(1.0);
                    tarjetaNode.setDisable(false);
                 });
            } catch (Exception e) { // Captura genérica para otros errores (ej. IllegalArgumentException del backend)
                 Platform.runLater(() -> {
                    handleError("Error al marcar pedido: " + e.getMessage(), e);
                    tarjetaNode.setOpacity(1.0);
                    tarjetaNode.setDisable(false);
                 });
            }
        }).start();
    }

    private void handleError(String message, Exception e) {
        System.err.println(message);
        if (e != null) { e.printStackTrace(); }
        statusLabelKitchen.setText(message);
        statusLabelKitchen.getStyleClass().setAll("lbl-danger");
    }

    public void cleanup() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            System.out.println("Timeline de cocina detenido.");
        }
    }
}