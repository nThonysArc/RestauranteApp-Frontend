package proyectopos.restauranteappfrontend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.CategoriaService;
import proyectopos.restauranteappfrontend.services.HttpClientService;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.services.PedidoMesaService;
import proyectopos.restauranteappfrontend.services.ProductoService;
import proyectopos.restauranteappfrontend.util.SessionManager;

public class DashboardController {

    // --- Elementos FXML ---
    @FXML private Label infoLabel;
    @FXML private TilePane mesasContainer;
    @FXML private ListView<CategoriaDTO> categoriasListView;
    @FXML private ListView<CategoriaDTO> subCategoriasListView;
    @FXML private TableView<ProductoDTO> productosTableView;
    @FXML private TableColumn<ProductoDTO, String> nombreProductoCol;
    @FXML private TableColumn<ProductoDTO, Double> precioProductoCol;
    @FXML private TableColumn<ProductoDTO, String> categoriaProductoCol;

    // --- Pedido Actual ---
    @FXML private Label mesaSeleccionadaLabel; // ⬅️ AÑADIDO para mostrar la mesa
    @FXML private TableView<DetallePedidoMesaDTO> pedidoActualTableView;
    @FXML private TableColumn<DetallePedidoMesaDTO, String> pedidoNombreCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Integer> pedidoCantidadCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoPrecioCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoSubtotalCol;
    @FXML private Label subTotalPedidoLabel; // ⬅️ AÑADIDO
    @FXML private Label igvPedidoLabel; // ⬅️ AÑADIDO
    @FXML private Label totalPedidoLabel;
    @FXML private Button crearPedidoButton;

    // --- Servicios ---
    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService();

    // --- Estado ---
    private MesaDTO mesaSeleccionada = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
    private final ObservableList<DetallePedidoMesaDTO> pedidoActualData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> categoriasData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> subCategoriasData = FXCollections.observableArrayList();
    private FilteredList<ProductoDTO> filteredProductos;


    @FXML
    public void initialize() {
        infoLabel.setText("Cargando datos iniciales...");
        mesaSeleccionadaLabel.setText("Mesa: (Ninguna)"); // Inicializar label de mesa

        configurarTablaProductos();
        configurarContenedorMesas();
        configurarTablaPedidoActual();
        cargarDatosIniciales();
        configurarSeleccionProducto();
        
        // --- MODIFICADO (PASO 3) ---
        // Esta llamada ahora SÓLO añadirá los botones si el rol es ADMIN
        configurarBotonesAdmin();
        // --- FIN MODIFICADO ---

        crearPedidoButton.setDisable(true);

        // --- Lógica de Listas de Categorías ---
        subCategoriasListView.setItems(subCategoriasData);
        subCategoriasListView.setPlaceholder(new Label("Seleccione una categoría principal"));

        categoriasListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, categoriaSeleccionada) -> {
                    subCategoriasData.clear();
                    subCategoriasListView.getSelectionModel().clearSelection(); // Deseleccionar subcategoría
                    // Resetear filtro productos al cambiar categoría principal
                    if (filteredProductos != null) {
                        filteredProductos.setPredicate(p -> true);
                    }


                    if (categoriaSeleccionada != null) {
                        infoLabel.setText("Categoría: " + categoriaSeleccionada.getNombre());
                        List<CategoriaDTO> subcategorias = categoriasData.stream()
                                .filter(c -> categoriaSeleccionada.getIdCategoria().equals(c.getIdCategoriaPadre()))
                                .collect(Collectors.toList());
                        subCategoriasData.addAll(subcategorias);
                        if(subcategorias.isEmpty()){
                            subCategoriasListView.setPlaceholder(new Label("No hay subcategorías"));
                        }
                    } else {
                        subCategoriasListView.setPlaceholder(new Label("Seleccione una categoría principal"));
                    }
                }
        );

        subCategoriasListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, subCategoriaSeleccionada) -> {
                    // Solo filtrar si la lista filtrada ya fue inicializada
                    if (filteredProductos == null) return;

                    if (subCategoriaSeleccionada != null) {
                        infoLabel.setText("Subcategoría: " + subCategoriaSeleccionada.getNombre());
                        filteredProductos.setPredicate(producto ->
                                producto.getIdCategoria().equals(subCategoriaSeleccionada.getIdCategoria())
                        );
                    } else {
                        // Si se deselecciona subcategoría pero AÚN hay categoría principal seleccionada
                        CategoriaDTO catPrincipal = categoriasListView.getSelectionModel().getSelectedItem();
                        if (catPrincipal != null) {
                            // Podríamos decidir mostrar todos los productos de la categoría principal
                            // o simplemente resetear a todos los productos (más simple)
                            filteredProductos.setPredicate(p -> true); // Resetear a todos
                            infoLabel.setText("Categoría: " + catPrincipal.getNombre()); // Volver a mostrar la principal
                        } else {
                            // Si no hay ni principal seleccionada, resetear a todos
                            filteredProductos.setPredicate(p -> true);
                        }

                    }
                }
        );
    }

    private void configurarContenedorMesas() {
        // Podrías configurar aquí el número de columnas del TilePane si es dinámico
        // mesasContainer.setPrefColumns(6); // Ejemplo
    }

    private void configurarTablaProductos() {
        nombreProductoCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        precioProductoCol.setCellValueFactory(new PropertyValueFactory<>("precio"));
        categoriaProductoCol.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));

        // Inicializar FilteredList aquí para evitar NullPointerException en el listener
        filteredProductos = new FilteredList<>(productosData, p -> true);
        productosTableView.setItems(filteredProductos);
    }

    private void configurarTablaPedidoActual() {
        pedidoNombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        pedidoCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        pedidoPrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        pedidoSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        pedidoActualTableView.setItems(pedidoActualData);

        pedidoActualTableView.setPlaceholder(new Label("Añada productos (doble clic)"));
    }

    private void configurarSeleccionProducto() {
        productosTableView.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                ProductoDTO productoSeleccionado = productosTableView.getSelectionModel().getSelectedItem();
                if (productoSeleccionado != null) {
                    handleSeleccionarProducto(productoSeleccionado);
                }
            }
        });
    }

    // --- MÉTODO MODIFICADO (PASO 3) ---
    private void configurarBotonesAdmin() {
        
        // 1. Obtener el rol de la sesión
        String userRole = SessionManager.getInstance().getRole();

        // 2. Comprobar si NO es Admin
        if (userRole == null || !userRole.equals("ROLE_ADMIN")) {
            // Si no es admin, no crear ni añadir los botones.
            System.out.println("Ocultando botones de admin. Rol de usuario: " + userRole); // Log de depuración
            return; 
        }
        
        // 3. Si llegamos aquí, ES ADMIN. Creamos los botones como antes.
        System.out.println("Mostrando botones de admin. Rol de usuario: " + userRole); // Log de depuración
        
        Button crearProductoBtn = new Button("Crear Producto");
        crearProductoBtn.getStyleClass().addAll("btn", "btn-info");
        crearProductoBtn.setOnAction(e -> handleCrearProducto());
        crearProductoBtn.setMaxWidth(Double.MAX_VALUE);
        // crearProductoBtn.setGraphic(new FontIcon(FontAwesomeSolid.PLUS)); // Ejemplo Ikonli

        Button gestionarCategoriasBtn = new Button("Gestionar Categorías");
        gestionarCategoriasBtn.getStyleClass().addAll("btn", "btn-secondary");
        gestionarCategoriasBtn.setOnAction(e -> handleGestionarCategorias());
        gestionarCategoriasBtn.setMaxWidth(Double.MAX_VALUE);
        // gestionarCategoriasBtn.setGraphic(new FontIcon(FontAwesomeSolid.SITEMAP)); // Ejemplo Ikonli


        HBox adminButtonContainer = new HBox(10, crearProductoBtn, gestionarCategoriasBtn);
        adminButtonContainer.setAlignment(Pos.CENTER_LEFT);
        adminButtonContainer.setPadding(new Insets(0, 0, 5, 0));

        try {
            // Asumiendo que el TableView está dentro de un VBox en el FXML
            Node parent = productosTableView.getParent();
            if (parent instanceof VBox) {
                VBox parentVBox = (VBox) parent;
                // Insertar después del Label "Productos" (que asumimos está en índice 0)
                if (parentVBox.getChildren().size() > 1) { // Asegurarse que hay espacio
                    parentVBox.getChildren().add(1, adminButtonContainer);
                } else {
                    parentVBox.getChildren().add(adminButtonContainer); // Añadir al final si no
                }

            } else {
                System.err.println("Advertencia: No se pudo encontrar el VBox padre de la tabla de productos para añadir botones admin.");
            }

        } catch (Exception e) {
            System.err.println("Error al intentar añadir botones de admin a la UI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // --- FIN MÉTODO MODIFICADO ---


    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos iniciales...");
        infoLabel.getStyleClass().setAll("lbl-warning");

        new Thread(() -> {
            // ... (Lógica de carga de datos sin cambios) ...
            List<MesaDTO> mesas = null;
            List<CategoriaDTO> categorias = null;
            List<ProductoDTO> productos = null;
            String errorMessage = null;
            Exception caughtException = null;

            try {
                mesas = mesaService.getAllMesas();
                categorias = categoriaService.getAllCategorias();
                productos = productoService.getAllProductos();
            } catch (HttpClientService.AuthenticationException e) {
                errorMessage = "Error de autenticación: Sesión inválida o expirada.";
                caughtException = e;
            } catch (JsonSyntaxException e) {
                errorMessage = "Error al procesar respuesta del servidor (formato JSON inválido).";
                caughtException = e;
            } catch (IOException e) {
                errorMessage = "Error de conexión con el servidor: " + e.getMessage();
                caughtException = e;
            } catch (InterruptedException e) {
                errorMessage = "Carga de datos interrumpida.";
                caughtException = e;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                errorMessage = "Error inesperado al cargar datos.";
                caughtException = e;
            }

            // ... (Resto de la lógica de actualización UI sin cambios) ...
            final List<MesaDTO> finalMesas = mesas;
            final List<CategoriaDTO> finalCategorias = categorias;
            final List<ProductoDTO> finalProductos = productos;
            final String finalErrorMessage = errorMessage;
            final Exception finalCaughtException = caughtException;

            Platform.runLater(() -> {
                if (finalErrorMessage == null) {
                    mostrarMesas(finalMesas);
                    mostrarCategorias(finalCategorias);
                    mostrarProductos(finalProductos);
                    infoLabel.setText("Datos cargados correctamente.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                } else {
                    infoLabel.setText(finalErrorMessage);
                    infoLabel.getStyleClass().setAll("lbl-danger");
                    mesasContainer.getChildren().clear();
                    mesasContainer.getChildren().add(new Label("Error al cargar mesas"));
                    categoriasListView.getItems().clear();
                    categoriasListView.setPlaceholder(new Label("Error al cargar categorías"));
                    subCategoriasListView.getItems().clear(); // Limpiar también
                    subCategoriasListView.setPlaceholder(new Label("Error al cargar"));
                    productosData.clear();
                    productosTableView.setPlaceholder(new Label("Error al cargar productos"));

                    if (finalCaughtException != null) {
                        finalCaughtException.printStackTrace();
                    }
                    if (finalCaughtException instanceof HttpClientService.AuthenticationException) {
                        handleAuthenticationError((HttpClientService.AuthenticationException) finalCaughtException);
                    } else {
                        handleGenericError(finalErrorMessage, finalCaughtException);
                    }
                }
            });
        }).start();
    }

    // --- MODIFICADO: Mostrar Mesas con Estilos CSS ---
    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear();
        if (mesas != null && !mesas.isEmpty()) {
            for (MesaDTO mesa : mesas) {
                Button mesaButton = new Button();
                mesaButton.setUserData(mesa);
                mesaButton.getStyleClass().add("mesa-button"); // Clase base

                // Contenido del botón (Número y Estado/Total)
                VBox buttonContent = new VBox(-2); // Espaciado negativo ligero
                buttonContent.setAlignment(Pos.CENTER);
                Text numeroMesaText = new Text(String.valueOf(mesa.getNumeroMesa()));
                numeroMesaText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
                Text estadoMesaText = new Text();
                estadoMesaText.setStyle("-fx-font-size: 10px;");

                // Aplicar estilo y texto según el estado
                switch (mesa.getEstado()) {
                    case "DISPONIBLE":
                        mesaButton.getStyleClass().add("mesa-libre");
                        estadoMesaText.setText("Libre");
                        mesaButton.setOnAction(event -> { // Habilitar acción solo si está libre
                            MesaDTO mesaData = (MesaDTO) ((Button) event.getSource()).getUserData();
                            handleSeleccionarMesa(mesaData);
                        });
                        break;
                    case "OCUPADA":
                        mesaButton.getStyleClass().add("mesa-ocupada");
                        // Aquí necesitaríamos buscar el total del pedido actual para esta mesa
                        // Por ahora, solo ponemos "Ocupada"
                        estadoMesaText.setText("Ocupada");
                        // Opcional: acción para ver/editar pedido existente
                        mesaButton.setOnAction(event -> {
                            MesaDTO mesaData = (MesaDTO) ((Button) event.getSource()).getUserData();
                            handleSeleccionarMesa(mesaData); // Permitir seleccionar para ver
                        });
                        break;
                    case "RESERVADA": // Asumiendo este estado existe
                        mesaButton.getStyleClass().add("mesa-reservada");
                        estadoMesaText.setText("Reservada");
                        mesaButton.setDisable(true); // Deshabilitar botón
                        break;
                    // Añadir caso "PAGANDO" si lo implementas
                    // case "PAGANDO":
                    //     mesaButton.getStyleClass().add("mesa-pagando");
                    //     estadoMesaText.setText("Pagando");
                    //     // Acción para procesar pago
                    //     break;
                    default:
                        mesaButton.getStyleClass().add("btn-secondary"); // Estilo por defecto
                        estadoMesaText.setText(mesa.getEstado());
                        mesaButton.setDisable(true);
                }
                // Ajustar color de texto si el fondo lo requiere (ej. amarillo)
                if ("PAGANDO".equals(mesa.getEstado())) {
                    numeroMesaText.setStyle(numeroMesaText.getStyle() + "; -fx-fill: #111827;");
                    estadoMesaText.setStyle(estadoMesaText.getStyle() + "; -fx-fill: #111827;");
                } else {
                    numeroMesaText.setStyle(numeroMesaText.getStyle() + "; -fx-fill: white;");
                    estadoMesaText.setStyle(estadoMesaText.getStyle() + "; -fx-fill: white;");
                }


                buttonContent.getChildren().addAll(numeroMesaText, estadoMesaText);
                mesaButton.setGraphic(buttonContent); // Usar VBox como gráfico

                mesasContainer.getChildren().add(mesaButton);
            }
        } else {
            mesasContainer.getChildren().add(new Label("No se encontraron mesas."));
        }
    }

    // --- MODIFICADO: Seleccionar Mesa ---
    private void handleSeleccionarMesa(MesaDTO mesa) {
        pedidoActualData.clear(); // Limpiar siempre al cambiar de mesa
        actualizarTotalPedido();
        actualizarEstadoCrearPedidoButton(); // Deshabilitar botón

        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.mesaSeleccionada = mesa;
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Nueva Orden)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada. Añada productos al pedido.");
            infoLabel.getStyleClass().setAll("lbl-info");
            actualizarEstadoCrearPedidoButton(); // Habilitar si hay items (aunque estará vacío al inicio)
        } else if ("OCUPADA".equals(mesa.getEstado())) {
            this.mesaSeleccionada = mesa; // Permitir seleccionar para ver/editar
            mesaSeleccionadaLabel.setText("Mesa: " + mesa.getNumeroMesa() + " (Orden Activa)");
            infoLabel.setText("Viendo pedido de Mesa " + mesa.getNumeroMesa() + ". (Funcionalidad de editar/añadir pendiente)");
            infoLabel.getStyleClass().setAll("lbl-warning");
            // TODO: Cargar los items del pedido existente para esta mesa
            // Necesitarías un endpoint en el backend para "GET /api/pedidosMesa?mesaId={id}&estado=ABIERTO"
            // y luego poblar pedidoActualData con los detalles recibidos.
            crearPedidoButton.setDisable(true); // Deshabilitar "Crear" si es un pedido existente
        } else {
            // Otros estados (Reservada, Pagando)
            this.mesaSeleccionada = null;
            mesaSeleccionadaLabel.setText("Mesa: (Ninguna)");
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " está " + mesa.getEstado() + ".");
            infoLabel.getStyleClass().setAll("lbl-warning");
        }
    }

    // --- Seleccionar Producto (Sin cambios) ---
    private void handleSeleccionarProducto(ProductoDTO producto) {
        if (mesaSeleccionada == null || !"DISPONIBLE".equals(mesaSeleccionada.getEstado())) { // Solo añadir a mesas disponibles
            mostrarAlerta("Acción no permitida", "Seleccione una mesa LIBRE para añadir productos.");
            return;
        }
        // ... (resto del método sin cambios) ...
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Añadir Producto");
        dialog.setHeaderText("Añadir '" + producto.getNombre() + "' al pedido");
        dialog.setContentText("Cantidad:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidadStr -> {
            try {
                int cantidad = Integer.parseInt(cantidadStr);
                if (cantidad > 0) {
                    DetallePedidoMesaDTO detalle = new DetallePedidoMesaDTO();
                    detalle.setIdProducto(producto.getIdProducto());
                    detalle.setNombreProducto(producto.getNombre());
                    detalle.setCantidad(cantidad);
                    detalle.setPrecioUnitario(producto.getPrecio());

                    pedidoActualData.add(detalle);
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton();

                } else {
                    mostrarAlerta("Cantidad inválida", "La cantidad debe ser un número positivo.");
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Entrada inválida", "Por favor, ingrese un número válido para la cantidad.");
            }
        });
    }

    // --- Gestionar Categorías (Sin cambios) ---
    @FXML
    private void handleGestionarCategorias() {
        // ... (sin cambios) ...
        Dialog<CategoriaDTO> dialog = new Dialog<>();
        dialog.setTitle("Gestionar Categorías");
        dialog.setHeaderText("Crear nueva categoría o subcategoría.");

        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre (Ej. Gaseosas o Platos Marinos)");

        ComboBox<CategoriaDTO> categoriaPadreComboBox = new ComboBox<>();
        ObservableList<CategoriaDTO> categoriasPadre = categoriasData.stream()
                .filter(c -> c.getIdCategoriaPadre() == null)
                // --- CORRECCIÓN DE ERROR AQUÍ ---
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
                // --- FIN DE CORRECCIÓN ---

        categoriaPadreComboBox.setItems(categoriasPadre);
        categoriaPadreComboBox.setPromptText("Opcional: Seleccione categoría padre");

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Categoría Padre:"), 0, 1);
        grid.add(categoriaPadreComboBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Node crearButton = dialog.getDialogPane().lookupButton(crearButtonType);
        crearButton.setDisable(true);
        nombreField.textProperty().addListener((o, ov, nv) -> {
            crearButton.setDisable(nv.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                CategoriaDTO nuevaCategoria = new CategoriaDTO();
                nuevaCategoria.setNombre(nombreField.getText().trim());

                CategoriaDTO padreSeleccionado = categoriaPadreComboBox.getValue();
                if (padreSeleccionado != null) {
                    nuevaCategoria.setIdCategoriaPadre(padreSeleccionado.getIdCategoria());
                } else {
                    nuevaCategoria.setIdCategoriaPadre(null);
                }
                return nuevaCategoria;
            }
            return null;
        });

        Optional<CategoriaDTO> result = dialog.showAndWait();
        result.ifPresent(this::llamarCrearCategoriaApi);
    }

    // --- Llamar API Crear Categoría (Sin cambios) ---
    private void llamarCrearCategoriaApi(CategoriaDTO categoriaACrear) {
        // ... (sin cambios) ...
        infoLabel.setText("Creando categoría '" + categoriaACrear.getNombre() + "'...");
        infoLabel.getStyleClass().setAll("lbl-warning");

        new Thread(() -> {
            try {
                CategoriaDTO categoriaCreada = categoriaService.crearCategoria(categoriaACrear);
                Platform.runLater(() -> {
                    infoLabel.setText("Categoría '" + categoriaCreada.getNombre() + "' creada.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                    cargarDatosIniciales();
                });
            } catch (Exception e) {
                Platform.runLater(() -> handleGenericError("Error al crear la categoría", e));
            }
        }).start();
    }


    // --- Crear Producto (Sin cambios) ---
    @FXML
    private void handleCrearProducto() {
        // ... (sin cambios) ...
        if (categoriasData.isEmpty()) {
            mostrarAlerta("Error", "Las categorías aún no se han cargado. Espere e intente de nuevo.");
            return;
        }

        Dialog<ProductoDTO> dialog = new Dialog<>();
        dialog.setTitle("Crear Nuevo Producto");
        dialog.setHeaderText("Ingrese los detalles del nuevo producto (plato, bebida, etc.).");

        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre del plato (ej. Lomo Saltado)");
        TextArea descripcionField = new TextArea();
        descripcionField.setPromptText("Descripción (ej. Trozos de lomo fino...)");
        descripcionField.setWrapText(true);
        descripcionField.setPrefRowCount(3);
        TextField precioField = new TextField();
        precioField.setPromptText("Precio (ej. 25.50)");

        ComboBox<CategoriaDTO> comboCategoriaPadre = new ComboBox<>();
        ObservableList<CategoriaDTO> categoriasPadre = categoriasData.stream()
                .filter(c -> c.getIdCategoriaPadre() == null)
                // --- CORRECCIÓN DE ERROR AQUÍ (Identico al anterior) ---
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
                // --- FIN DE CORRECCIÓN ---
        comboCategoriaPadre.setItems(categoriasPadre);
        comboCategoriaPadre.setPromptText("Seleccione categoría principal");

        ComboBox<CategoriaDTO> comboSubcategoria = new ComboBox<>();
        comboSubcategoria.setPromptText("Seleccione subcategoría");
        comboSubcategoria.setDisable(true);

        comboCategoriaPadre.valueProperty().addListener((observable, oldValue, categoriaPadreSeleccionada) -> {
            comboSubcategoria.getItems().clear();
            comboSubcategoria.setValue(null);

            if (categoriaPadreSeleccionada != null) {
                ObservableList<CategoriaDTO> subcategoriasFiltradas = categoriasData.stream()
                        .filter(sub -> categoriaPadreSeleccionada.getIdCategoria().equals(sub.getIdCategoriaPadre()))
                        // --- CORRECCIÓN DE ERROR AQUÍ (Tercera instancia) ---
                        .collect(Collectors.toCollection(FXCollections::observableArrayList));
                        // --- FIN DE CORRECCIÓN ---

                if (!subcategoriasFiltradas.isEmpty()) {
                    comboSubcategoria.setItems(subcategoriasFiltradas);
                    comboSubcategoria.setDisable(false);
                } else {
                    comboSubcategoria.setPromptText("No hay subcategorías");
                    comboSubcategoria.setDisable(true);
                }
            } else {
                comboSubcategoria.setDisable(true);
            }
        });

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descripcionField, 1, 1);
        grid.add(new Label("Precio:"), 0, 2);
        grid.add(precioField, 1, 2);
        grid.add(new Label("Categoría:"), 0, 3);
        grid.add(comboCategoriaPadre, 1, 3);
        grid.add(new Label("Subcategoría:"), 0, 4);
        grid.add(comboSubcategoria, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Node crearButton = dialog.getDialogPane().lookupButton(crearButtonType);
        crearButton.setDisable(true);

        Runnable validador = () -> {
            boolean invalido = nombreField.getText().trim().isEmpty()
                    || precioField.getText().trim().isEmpty()
                    || comboSubcategoria.getValue() == null;
            crearButton.setDisable(invalido);
        };

        nombreField.textProperty().addListener((o, ov, nv) -> validador.run());
        precioField.textProperty().addListener((o, ov, nv) -> validador.run());
        comboSubcategoria.valueProperty().addListener((o, ov, nv) -> validador.run());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                try {
                    ProductoDTO nuevoProducto = new ProductoDTO();
                    nuevoProducto.setNombre(nombreField.getText().trim());
                    nuevoProducto.setDescripcion(descripcionField.getText().trim());

                    double precio = Double.parseDouble(precioField.getText().trim());
                    if (precio <= 0) throw new NumberFormatException("El precio debe ser positivo");
                    nuevoProducto.setPrecio(precio);

                    nuevoProducto.setIdCategoria(comboSubcategoria.getValue().getIdCategoria());

                    return nuevoProducto;
                } catch (NumberFormatException e) {
                    mostrarAlerta("Datos Inválidos", "El precio debe ser un número positivo (ej. 25.50).");
                    return null;
                }
            }
            return null;
        });

        Optional<ProductoDTO> result = dialog.showAndWait();

        result.ifPresent(productoACrear -> {
            if (productoACrear == null) return;

            infoLabel.setText("Creando producto '" + productoACrear.getNombre() + "'...");
            infoLabel.getStyleClass().setAll("lbl-warning");

            new Thread(() -> {
                try {
                    ProductoDTO productoCreado = productoService.crearProducto(productoACrear);

                    Platform.runLater(() -> {
                        infoLabel.setText("Producto '" + productoCreado.getNombre() + "' creado con éxito.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        cargarDatosIniciales();
                    });

                } catch (HttpClientService.AuthenticationException e) {
                    Platform.runLater(() -> {
                        handleAuthenticationError(e);
                        mostrarAlerta("Acceso Denegado", "No tiene permisos de Administrador para crear productos.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleGenericError("Error al crear el producto", e);
                    });
                }
            }).start();
        });
    }


    // --- MODIFICADO: Actualizar Total Pedido (Calcula IGV) ---
    private void actualizarTotalPedido() {
        double subtotal = 0.0;
        for (DetallePedidoMesaDTO detalle : pedidoActualData) {
            subtotal += detalle.getSubtotal();
        }
        double igv = subtotal * 0.18; // Asumiendo IGV del 18%
        double total = subtotal + igv;

        subTotalPedidoLabel.setText(String.format("S/ %.2f", subtotal));
        igvPedidoLabel.setText(String.format("S/ %.2f", igv));
        totalPedidoLabel.setText(String.format("S/ %.2f", total));
    }

    // --- Actualizar Estado Botón Pedido (Sin cambios) ---
    private void actualizarEstadoCrearPedidoButton() {
        // Habilitar solo si hay mesa LIBRE seleccionada y hay items
        crearPedidoButton.setDisable(mesaSeleccionada == null || !"DISPONIBLE".equals(mesaSeleccionada.getEstado()) || pedidoActualData.isEmpty());
    }

    // --- Crear Pedido (Sin cambios en la lógica principal) ---
    @FXML
    private void handleCrearPedido() {
        if (mesaSeleccionada == null || !"DISPONIBLE".equals(mesaSeleccionada.getEstado()) || pedidoActualData.isEmpty()) {
            mostrarAlerta("Pedido incompleto", "Debe seleccionar una mesa LIBRE y añadir al menos un producto.");
            return;
        }
        // ... (resto del método sin cambios) ...
        infoLabel.setText("Creando pedido...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        crearPedidoButton.setDisable(true);

        PedidoMesaDTO nuevoPedido = new PedidoMesaDTO();
        nuevoPedido.setIdMesa(mesaSeleccionada.getIdMesa());
        nuevoPedido.setEstado("ABIERTO");
        nuevoPedido.setDetalles(new ArrayList<>(pedidoActualData));

        new Thread(() -> {
            try {
                PedidoMesaDTO pedidoCreado = pedidoMesaService.crearPedido(nuevoPedido);

                Platform.runLater(() -> {
                    infoLabel.setText("Pedido #" + pedidoCreado.getIdPedidoMesa() + " creado exitosamente para Mesa " + mesaSeleccionada.getNumeroMesa());
                    infoLabel.getStyleClass().setAll("lbl-success");
                    pedidoActualData.clear();
                    mesaSeleccionada = null;
                    mesaSeleccionadaLabel.setText("Mesa: (Ninguna)"); // Resetear label
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton();
                    cargarDatosIniciales(); // Recargar todo, incluyendo estado de mesas
                });
            } catch (HttpClientService.AuthenticationException e) {
                Platform.runLater(() -> {
                    handleAuthenticationError(e);
                    crearPedidoButton.setDisable(false); // Rehabilitar en caso de error
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    handleGenericError("Error al crear el pedido", e);
                    crearPedidoButton.setDisable(false); // Rehabilitar en caso de error
                });
            }
        }).start();

    }

    // --- Alertas (Sin cambios) ---
    private void mostrarAlerta(String titulo, String contenido) {
        // ... (sin cambios) ...
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void mostrarAlertaError(String titulo, String contenido) {
        // ... (sin cambios) ...
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }


    // --- Mostrar Categorías (Sin cambios) ---
    private void mostrarCategorias(List<CategoriaDTO> categorias) {
        // ... (sin cambios) ...
        categoriasData.clear();

        if (categorias != null && !categorias.isEmpty()) {
            categoriasData.addAll(categorias);

            List<CategoriaDTO> categoriasPrincipales = categorias.stream()
                    .filter(c -> c.getIdCategoriaPadre() == null)
                    .collect(Collectors.toList());

            categoriasListView.setItems(FXCollections.observableArrayList(categoriasPrincipales));

        } else {
            categoriasListView.setPlaceholder(new Label("No se encontraron categorías."));
        }
    }

    // --- Mostrar Productos (Sin cambios) ---
    private void mostrarProductos(List<ProductoDTO> productos) {
        // ... (sin cambios) ...
        productosData.clear();
        if (productos != null && !productos.isEmpty()) {
            productosData.addAll(productos);
        }
        if (productosData.isEmpty()) {
            productosTableView.setPlaceholder(new Label("No se encontraron productos."));
        }
        // Asegurarse de resetear el predicado si la lista estaba vacía antes
        if (filteredProductos != null) {
            filteredProductos.setPredicate(p -> true);
        }
    }

    // --- Manejo de Errores (Sin cambios) ---
    private void handleAuthenticationError(HttpClientService.AuthenticationException e) {
        // ... (sin cambios) ...
        infoLabel.setText(e.getMessage());
        infoLabel.getStyleClass().setAll("lbl-danger");
        System.err.println("Error de autenticación, se debería redirigir al login.");
        mostrarAlerta("Sesión Expirada", "Su sesión ha expirado o no es válida. Por favor, vuelva a iniciar sesión.");
        
        // --- MODIFICACIÓN (PASO 3) ---
        // Si hay un error de autenticación, deberíamos cerrar la sesión
        // y volver al login. Esta lógica ya está en MainController,
        // pero podríamos necesitar una forma de llamarla desde aquí.
        
        // (Por ahora, lo dejamos así, pero idealmente esto
        // llamaría a un método en MainController para cerrar sesión)
    }

    private void handleGenericError(String message, Exception e) {
        // ... (sin cambios) ...
        String errorMessage = (message != null && !message.isBlank()) ? message : "Error inesperado.";
        if (e != null) {
            errorMessage += ": " + e.getMessage();
            e.printStackTrace();
        }

        infoLabel.setText(errorMessage);
        infoLabel.getStyleClass().setAll("lbl-danger");
        mostrarAlertaError("Error", errorMessage);
    }
}