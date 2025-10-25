package proyectopos.restauranteappfrontend.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos; // <-- AÑADIR IMPORT
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox; // <-- AÑADIR IMPORT
import javafx.util.Callback; // <-- AÑADIR IMPORT
import proyectopos.restauranteappfrontend.model.dto.EmpleadoDTO;
import proyectopos.restauranteappfrontend.model.dto.RolDTO;
import proyectopos.restauranteappfrontend.services.EmpleadoService;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.RolService;

import java.io.IOException;
import java.util.List;
import java.util.Optional; // <-- AÑADIR IMPORT
import java.util.regex.Pattern;

public class EmployeeManagementController {

    // --- Campos del Formulario ---
    @FXML private Label formTitleLabel; // <-- NUEVO
    @FXML private TextField nombreField;
    @FXML private TextField dniField;
    @FXML private TextField usuarioField;
    @FXML private PasswordField contrasenaField;
    @FXML private ComboBox<RolDTO> rolComboBox;
    @FXML private Button saveButton; // <-- Renombrado (antes crearButton)
    @FXML private Button cancelEditButton; // <-- NUEVO
    @FXML private Label statusLabel;
    @FXML private HBox formButtonsBox; // <-- NUEVO

    // --- Campos Tabla ---
    @FXML private TableView<EmpleadoDTO> empleadosTableView;
    @FXML private TableColumn<EmpleadoDTO, String> nombreCol;
    @FXML private TableColumn<EmpleadoDTO, String> dniCol;
    @FXML private TableColumn<EmpleadoDTO, String> usuarioCol;
    @FXML private TableColumn<EmpleadoDTO, String> rolCol;
    @FXML private TableColumn<EmpleadoDTO, Void> accionesCol; // <-- TIPO CAMBIADO A Void

    // --- Servicios (sin cambios) ---
    private final EmpleadoService empleadoService = new EmpleadoService();
    private final RolService rolService = new RolService();

    // --- ObservableList Tabla (sin cambios) ---
    private ObservableList<EmpleadoDTO> empleadosData = FXCollections.observableArrayList();

    // Lista de roles (sin cambios)
    private List<RolDTO> rolesDisponibles;

    // --- Patrón DNI (sin cambios) ---
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");

    // --- NUEVO: Estado para saber si estamos editando ---
    private boolean modoEdicion = false;
    private EmpleadoDTO empleadoEnEdicion = null;


    @FXML
    public void initialize() {
        statusLabel.setText("");
        cargarRoles();
        configurarTablaEmpleados(); // Ahora también configura la columna de acciones
        cargarEmpleados();

        // TextFormatter DNI (sin cambios)
        dniField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d{0,8}")) { return change; }
            return null;
        }));

        // Listeners para habilitar botón (sin cambios)
        nombreField.textProperty().addListener((obs, old, nw) -> validarFormularioYActualizarBoton());
        dniField.textProperty().addListener((obs, old, nw) -> validarFormularioYActualizarBoton());
        usuarioField.textProperty().addListener((obs, old, nw) -> validarFormularioYActualizarBoton());
        contrasenaField.textProperty().addListener((obs, old, nw) -> validarFormularioYActualizarBoton());
        rolComboBox.valueProperty().addListener((obs, old, nw) -> validarFormularioYActualizarBoton());

        // Validar estado inicial
        validarFormularioYActualizarBoton();

        // Estado inicial del formulario (modo creación)
        prepararFormularioParaCreacion();
    }

    // --- MODIFICADO: Configurar Tabla (añade CellFactory para botones) ---
    private void configurarTablaEmpleados() {
        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        dniCol.setCellValueFactory(new PropertyValueFactory<>("dni"));
        usuarioCol.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        rolCol.setCellValueFactory(new PropertyValueFactory<>("rolNombre"));
        empleadosTableView.setItems(empleadosData);

        // --- Configuración de la Columna de Acciones ---
        Callback<TableColumn<EmpleadoDTO, Void>, TableCell<EmpleadoDTO, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<EmpleadoDTO, Void> call(final TableColumn<EmpleadoDTO, Void> param) {
                final TableCell<EmpleadoDTO, Void> cell = new TableCell<>() {

                    private final Button btnEditar = new Button("Editar");
                    private final Button btnEliminar = new Button("Eliminar");
                    private final HBox pane = new HBox(5, btnEditar, btnEliminar); // HBox para los botones

                    {
                        btnEditar.getStyleClass().addAll("btn-info", "btn-sm"); // Estilos BootstrapFX pequeños
                        btnEliminar.getStyleClass().addAll("btn-danger", "btn-sm");
                        pane.setAlignment(Pos.CENTER); // Centrar botones en la celda

                        btnEditar.setOnAction(event -> {
                            EmpleadoDTO empleado = getTableView().getItems().get(getIndex());
                            prepararFormularioParaEdicion(empleado);
                        });

                        btnEliminar.setOnAction(event -> {
                            EmpleadoDTO empleado = getTableView().getItems().get(getIndex());
                            handleEliminarEmpleado(empleado);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
                return cell;
            }
        };

        accionesCol.setCellFactory(cellFactory);
        // --- Fin Configuración Columna Acciones ---
    }

    // --- Método cargarEmpleados (sin cambios) ---
    private void cargarEmpleados() {
        empleadosTableView.setPlaceholder(new Label("Cargando empleados..."));
        new Thread(() -> {
            try {
                List<EmpleadoDTO> empleados = empleadoService.getAllEmpleados();
                Platform.runLater(() -> {
                    empleadosData.clear();
                    if (empleados != null && !empleados.isEmpty()) { empleadosData.addAll(empleados); }
                    empleadosTableView.setPlaceholder(new Label(empleadosData.isEmpty() ? "No hay empleados registrados." : "Error al cargar."));
                });
            } catch (Exception e) {
                 Platform.runLater(() -> {
                    handleError("Error al cargar empleados", e);
                    empleadosTableView.setPlaceholder(new Label("Error al cargar empleados."));
                 });
            }
        }).start();
    }

    // --- Método validarFormularioYActualizarBoton (sin cambios) ---
    private void validarFormularioYActualizarBoton() {
        String mensajeValidacion = validarFormulario();
        saveButton.setDisable(mensajeValidacion != null);
        if (mensajeValidacion == null && statusLabel.getStyleClass().contains("lbl-danger")) {
             statusLabel.setText("");
             statusLabel.getStyleClass().clear();
        }
    }

    // --- MODIFICADO: Validar Formulario (Contraseña opcional en edición) ---
    private String validarFormulario() {
        if (nombreField.getText().isBlank()) return "El nombre completo es obligatorio.";
        if (dniField.getText().isBlank()) return "El DNI es obligatorio.";
        if (!DNI_PATTERN.matcher(dniField.getText()).matches()) return "El DNI debe tener 8 dígitos numéricos.";
        if (usuarioField.getText().isBlank()) return "El nombre de usuario es obligatorio.";
        if (usuarioField.getText().contains(" ")) return "El nombre de usuario no debe contener espacios.";
        // Contraseña: Obligatoria solo si NO estamos en modo edición
        if (!modoEdicion && contrasenaField.getText().isBlank()) return "La contraseña es obligatoria para nuevos empleados.";
        // Contraseña: Si se ingresa (incluso en edición), debe tener >= 6 caracteres
        if (!contrasenaField.getText().isBlank() && contrasenaField.getText().length() < 6) return "La contraseña debe tener al menos 6 caracteres.";
        if (rolComboBox.getValue() == null) return "Debe seleccionar un rol.";

        return null; // Todo válido
    }

    // --- Método cargarRoles (sin cambios) ---
    private void cargarRoles() {
        statusLabel.setText("Cargando roles...");
        statusLabel.getStyleClass().setAll("lbl-info");
        new Thread(() -> {
            try {
                List<RolDTO> roles = rolService.getAllRoles();
                this.rolesDisponibles = roles;
                Platform.runLater(() -> {
                    if (roles != null && !roles.isEmpty()) {
                        rolComboBox.setItems(FXCollections.observableArrayList(roles));
                        statusLabel.setText("Roles cargados.");
                        validarFormularioYActualizarBoton();
                    } else { /* ... manejo de error ... */ }
                });
            } catch (Exception e) { /* ... manejo de error ... */ }
        }).start();
    }

    // --- MODIFICADO: Renombrado y ahora maneja CREAR y ACTUALIZAR ---
    @FXML
    private void handleGuardarEmpleado() {
        String mensajeError = validarFormulario();
        if (mensajeError != null) {
            statusLabel.setText(mensajeError);
            statusLabel.getStyleClass().setAll("lbl-danger");
            return;
        }

        // Construir DTO desde el formulario
        EmpleadoDTO empleadoDto = new EmpleadoDTO();
        empleadoDto.setNombre(nombreField.getText().trim());
        empleadoDto.setDni(dniField.getText().trim());
        empleadoDto.setUsuario(usuarioField.getText().trim());
        empleadoDto.setIdRol(rolComboBox.getValue().getIdRol());
        // Solo incluir contraseña si se escribió algo
        if (!contrasenaField.getText().isBlank()) {
            empleadoDto.setContrasena(contrasenaField.getText());
        }

        if (modoEdicion && empleadoEnEdicion != null) {
            // --- Lógica de ACTUALIZACIÓN ---
            statusLabel.setText("Actualizando empleado...");
            statusLabel.getStyleClass().setAll("lbl-warning");
            setFormDisabled(true);
            Long idParaActualizar = empleadoEnEdicion.getIdEmpleado(); // Guardar ID

            new Thread(() -> {
                try {
                    EmpleadoDTO empleadoActualizado = empleadoService.actualizarEmpleado(idParaActualizar, empleadoDto);
                    Platform.runLater(() -> {
                        statusLabel.setText("Empleado '" + empleadoActualizado.getUsuario() + "' actualizado.");
                        statusLabel.getStyleClass().setAll("lbl-success");
                        prepararFormularioParaCreacion(); // Volver a modo creación
                        setFormDisabled(false);
                        cargarEmpleados(); // Recargar tabla
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleError("Error al actualizar empleado", e);
                        setFormDisabled(false); // Habilitar formulario de nuevo
                    });
                }
            }).start();

        } else {
            // --- Lógica de CREACIÓN (como antes) ---
            statusLabel.setText("Creando empleado...");
            statusLabel.getStyleClass().setAll("lbl-warning");
            setFormDisabled(true);

            new Thread(() -> {
                try {
                    EmpleadoDTO empleadoCreado = empleadoService.crearEmpleado(empleadoDto);
                    Platform.runLater(() -> {
                        statusLabel.setText("Empleado '" + empleadoCreado.getUsuario() + "' creado.");
                        statusLabel.getStyleClass().setAll("lbl-success");
                        limpiarFormulario(); // Limpiar para nueva creación
                        setFormDisabled(false);
                        cargarEmpleados();
                    });
                } catch (Exception e) {
                     Platform.runLater(() -> {
                        String msg = "Error al crear empleado: ";
                         if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                             if (e.getMessage().contains("dni")) msg += "El DNI ya está registrado.";
                             else if (e.getMessage().contains("usuario")) msg += "El nombre de usuario ya está en uso.";
                             else msg += e.getMessage();
                         } else {
                             msg += (e.getMessage() != null ? e.getMessage() : "Error desconocido.");
                         }
                        handleError(msg, e);
                        setFormDisabled(false);
                     });
                }
            }).start();
        }
    }
    // --- FIN MÉTODO MODIFICADO ---

    // --- NUEVO MÉTODO: Manejar clic en botón Eliminar ---
    private void handleEliminarEmpleado(EmpleadoDTO empleado) {
        // Confirmación
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Eliminación");
        confirmacion.setHeaderText("Eliminar Empleado: " + empleado.getNombre());
        confirmacion.setContentText("¿Estás seguro de que deseas eliminar a este empleado?\nEsta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            statusLabel.setText("Eliminando empleado...");
            statusLabel.getStyleClass().setAll("lbl-warning");

            new Thread(() -> {
                try {
                    empleadoService.eliminarEmpleado(empleado.getIdEmpleado());
                    Platform.runLater(() -> {
                        statusLabel.setText("Empleado '" + empleado.getNombre() + "' eliminado.");
                        statusLabel.getStyleClass().setAll("lbl-success");
                        cargarEmpleados(); // Recargar la tabla
                        // Si estábamos editando este empleado, cancelar edición
                        if (modoEdicion && empleadoEnEdicion != null && empleadoEnEdicion.getIdEmpleado().equals(empleado.getIdEmpleado())) {
                            prepararFormularioParaCreacion();
                        }
                    });
                } catch (HttpClientService.AuthenticationException e) {
                    Platform.runLater(() -> handleError("Acceso denegado al eliminar", e));
                } catch (IOException | InterruptedException e) {
                    Platform.runLater(() -> handleError("Error de red al eliminar", e));
                } catch (Exception e) {
                     Platform.runLater(() -> handleError("Error al eliminar empleado", e));
                }
            }).start();
        }
    }
    // --- FIN NUEVO MÉTODO ---

    // --- NUEVO MÉTODO: Preparar formulario para Editar ---
    private void prepararFormularioParaEdicion(EmpleadoDTO empleado) {
        modoEdicion = true;
        empleadoEnEdicion = empleado;

        formTitleLabel.setText("Editar Empleado: " + empleado.getNombre());
        nombreField.setText(empleado.getNombre());
        dniField.setText(empleado.getDni());
        usuarioField.setText(empleado.getUsuario());
        contrasenaField.clear(); // Limpiar contraseña, solo se cambia si se escribe algo
        contrasenaField.setPromptText("Dejar en blanco para no cambiar"); // Cambiar prompt

        // Seleccionar el rol actual en el ComboBox
        if (rolesDisponibles != null) {
            Optional<RolDTO> rolActual = rolesDisponibles.stream()
                .filter(r -> r.getIdRol().equals(empleado.getIdRol()))
                .findFirst();
            rolActual.ifPresent(rolComboBox::setValue);
        }

        saveButton.setText("Guardar Cambios");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true); // Hacer que ocupe espacio

        statusLabel.setText("Editando empleado. Cambie los campos deseados y guarde.");
        statusLabel.getStyleClass().setAll("lbl-info");

        validarFormularioYActualizarBoton(); // Revalidar el botón
    }
    // --- FIN NUEVO MÉTODO ---

     // --- NUEVO MÉTODO: Preparar formulario para Crear (o cancelar edición) ---
    private void prepararFormularioParaCreacion() {
        modoEdicion = false;
        empleadoEnEdicion = null;

        formTitleLabel.setText("Crear Nuevo Empleado");
        limpiarFormulario(); // Limpia los campos
        contrasenaField.setPromptText("Mínimo 6 caracteres"); // Restaurar prompt

        saveButton.setText("Crear Empleado");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false); // Hacer que no ocupe espacio

        statusLabel.setText(""); // Limpiar mensaje de estado

        validarFormularioYActualizarBoton(); // Revalidar el botón (probablemente estará deshabilitado)
    }
    // --- FIN NUEVO MÉTODO ---

    // --- NUEVO MÉTODO: Manejar clic en botón Cancelar Edición ---
    @FXML
    private void handleCancelarEdicion() {
        prepararFormularioParaCreacion();
    }
    // --- FIN NUEVO MÉTODO ---


    // --- Métodos limpiarFormulario, setFormDisabled, handleError (sin cambios significativos) ---
    private void limpiarFormulario() {
        nombreField.clear();
        dniField.clear();
        usuarioField.clear();
        contrasenaField.clear();
        rolComboBox.getSelectionModel().clearSelection();
        // No limpiar statusLabel aquí si queremos mantener mensaje de éxito/error
        validarFormularioYActualizarBoton();
    }

    private void setFormDisabled(boolean disabled) {
        nombreField.setDisable(disabled);
        dniField.setDisable(disabled);
        usuarioField.setDisable(disabled);
        contrasenaField.setDisable(disabled);
        rolComboBox.setDisable(disabled);
        saveButton.setDisable(disabled);
        // Deshabilitar Cancelar también si el formulario principal está deshabilitado
        cancelEditButton.setDisable(disabled);
        // Deshabilitar la tabla mientras se edita/crea para evitar clics accidentales
        empleadosTableView.setDisable(disabled);
    }

    private void handleError(String message, Exception e) {
        System.err.println(message);
        if (e != null) { e.printStackTrace(); }
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll("lbl-danger");
    }
}