package proyectopos.restauranteappfrontend.controllers;

import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.services.MesaService;

public class TableManagementController {

    @FXML private TextField numeroMesaField;
    @FXML private TextField capacidadField;
    @FXML private ComboBox<String> estadoComboBox;
    @FXML private Label statusLabel;
    @FXML private Button guardarBtn;

    @FXML private TableView<MesaDTO> mesasTableView;
    @FXML private TableColumn<MesaDTO, Integer> colNumero;
    @FXML private TableColumn<MesaDTO, Integer> colCapacidad;
    @FXML private TableColumn<MesaDTO, String> colEstado;
    @FXML private TableColumn<MesaDTO, Void> colAcciones;

    private final MesaService mesaService = new MesaService();
    private final ObservableList<MesaDTO> mesasData = FXCollections.observableArrayList();
    private MesaDTO mesaEnEdicion = null;

    @FXML
    public void initialize() {
        // Configurar Combo de Estados
        estadoComboBox.setItems(FXCollections.observableArrayList("DISPONIBLE", "BLOQUEADA", "RESERVADA"));
        estadoComboBox.getSelectionModel().select("DISPONIBLE");

        // Configurar Tabla
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroMesa"));
        colCapacidad.setCellValueFactory(new PropertyValueFactory<>("capacidad"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        
        mesasTableView.setItems(mesasData);
        agregarBotonesAccion();
        cargarMesas();
    }

    private void cargarMesas() {
        new Thread(() -> {
            try {
                List<MesaDTO> mesas = mesaService.getAllMesas();
                Platform.runLater(() -> {
                    mesasData.clear();
                    mesasData.addAll(mesas);
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error al cargar mesas: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleGuardar() {
        try {
            Integer numero = Integer.parseInt(numeroMesaField.getText());
            Integer capacidad = Integer.parseInt(capacidadField.getText());
            String estado = estadoComboBox.getValue();

            MesaDTO mesaDTO = new MesaDTO();
            mesaDTO.setNumeroMesa(numero);
            mesaDTO.setCapacidad(capacidad);
            mesaDTO.setEstado(estado);

            new Thread(() -> {
                try {
                    if (mesaEnEdicion == null) {
                        // Crear nueva
                        mesaService.crearMesa(mesaDTO);
                        Platform.runLater(() -> {
                            statusLabel.setText("Mesa " + numero + " creada exitosamente.");
                            handleLimpiar();
                            cargarMesas();
                        });
                    } else {
                        // Actualizar existente
                        mesaService.actualizarMesa(mesaEnEdicion.getIdMesa(), mesaDTO);
                        Platform.runLater(() -> {
                            statusLabel.setText("Mesa " + numero + " actualizada.");
                            handleLimpiar();
                            cargarMesas();
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
                }
            }).start();

        } catch (NumberFormatException e) {
            statusLabel.setText("Por favor ingrese números válidos.");
        }
    }

    @FXML
    private void handleLimpiar() {
        numeroMesaField.clear();
        capacidadField.clear();
        estadoComboBox.getSelectionModel().select("DISPONIBLE");
        mesaEnEdicion = null;
        guardarBtn.setText("Guardar Mesa");
        statusLabel.setText("");
    }

    private void editarMesa(MesaDTO mesa) {
        mesaEnEdicion = mesa;
        numeroMesaField.setText(String.valueOf(mesa.getNumeroMesa()));
        capacidadField.setText(String.valueOf(mesa.getCapacidad()));
        estadoComboBox.setValue(mesa.getEstado());
        guardarBtn.setText("Actualizar Mesa");
        statusLabel.setText("Editando mesa " + mesa.getNumeroMesa());
    }

    private void agregarBotonesAccion() {
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            // Usamos "Bloquear" en lugar de eliminar para mantener historial
            private final Button btnBloquear = new Button("Ocultar/Bloquear"); 
            private final HBox pane = new HBox(5, btnEditar, btnBloquear);

            {
                btnEditar.getStyleClass().addAll("btn-sm", "btn-info");
                btnBloquear.getStyleClass().addAll("btn-sm", "btn-warning");

                btnEditar.setOnAction(event -> editarMesa(getTableView().getItems().get(getIndex())));
                
                btnBloquear.setOnAction(event -> {
                    MesaDTO mesa = getTableView().getItems().get(getIndex());
                    // Alternar estado
                    String nuevoEstado = "BLOQUEADA".equals(mesa.getEstado()) ? "DISPONIBLE" : "BLOQUEADA";
                    mesa.setEstado(nuevoEstado);
                    // Actualizar inmediatamente
                    new Thread(() -> {
                        try {
                            mesaService.actualizarMesa(mesa.getIdMesa(), mesa);
                            Platform.runLater(() -> cargarMesas());
                        } catch (Exception e) { e.printStackTrace(); }
                    }).start();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }
}