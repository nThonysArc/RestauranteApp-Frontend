package proyectopos.restauranteappfrontend.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import proyectopos.restauranteappfrontend.model.dto.RegistroSesionDTO;
import proyectopos.restauranteappfrontend.services.RegistroSesionService;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class SessionLogsController {

    @FXML private TextField usuarioFilterField;
    @FXML private DatePicker desdeDatePicker;
    @FXML private DatePicker hastaDatePicker;
    @FXML private Label statusLabel;

    @FXML private TableView<RegistroSesionDTO> logsTable;
    @FXML private TableColumn<RegistroSesionDTO, Long> colId;
    @FXML private TableColumn<RegistroSesionDTO, String> colFecha;
    @FXML private TableColumn<RegistroSesionDTO, String> colUsuario;
    @FXML private TableColumn<RegistroSesionDTO, String> colNombre;
    @FXML private TableColumn<RegistroSesionDTO, String> colRol;
    @FXML private TableColumn<RegistroSesionDTO, String> colIp;

    private final RegistroSesionService service = new RegistroSesionService();
    private final ObservableList<RegistroSesionDTO> logsData = FXCollections.observableArrayList();
    
    // Formateador para que la fecha se vea amigable en la tabla
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreEmpleado"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colIp.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));

        // Formatear la fecha que viene del backend (ISO) a algo legible
        colFecha.setCellValueFactory(cellData -> {
            String rawDate = cellData.getValue().getFechaLogin();
            try {
                // El backend envía LocalDateTime por defecto como ISO
                LocalDateTime ldt = LocalDateTime.parse(rawDate);
                return new javafx.beans.property.SimpleStringProperty(ldt.format(OUTPUT_FORMATTER));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty(rawDate);
            }
        });

        logsTable.setItems(logsData);
        handleBuscar(); // Cargar datos al inicio
    }

    @FXML
    private void handleBuscar() {
        statusLabel.setText("Cargando registros...");
        statusLabel.getStyleClass().setAll("lbl-info");

        // MODIFICADO: Uso de ThreadManager
        ThreadManager.getInstance().execute(() -> {
            try {
                List<RegistroSesionDTO> resultados = service.buscarSesiones(
                        usuarioFilterField.getText().trim(),
                        desdeDatePicker.getValue(),
                        hastaDatePicker.getValue()
                );

                Platform.runLater(() -> {
                    logsData.clear();
                    if (resultados != null) logsData.addAll(resultados);
                    statusLabel.setText("Registros cargados: " + logsData.size());
                    statusLabel.getStyleClass().setAll("lbl-success");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.getStyleClass().setAll("lbl-danger");
                    e.printStackTrace();
                });
            }
        });
    }

    @FXML
    private void handleLimpiar() {
        usuarioFilterField.clear();
        desdeDatePicker.setValue(null);
        hastaDatePicker.setValue(null);
        handleBuscar();
    }
}