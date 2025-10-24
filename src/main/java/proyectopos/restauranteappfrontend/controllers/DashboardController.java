package proyectopos.restauranteappfrontend.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton; // ⬅️ IMPORTAR para doble clic
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;
import proyectopos.restauranteappfrontend.model.dto.DetallePedidoMesaDTO; // ⬅️ IMPORTAR
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO; // ⬅️ IMPORTAR
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;
import proyectopos.restauranteappfrontend.services.*; // ⬅️ IMPORTAR (incluye PedidoMesaService)
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList; // ⬅️ IMPORTAR
import java.util.List;
import java.util.Optional; // ⬅️ IMPORTAR para diálogo

public class DashboardController {

    // --- Elementos FXML Existentes ---
    @FXML private Label infoLabel;
    @FXML private TilePane mesasContainer;
    @FXML private ListView<CategoriaDTO> categoriasListView;
    @FXML private TableView<ProductoDTO> productosTableView;
    @FXML private TableColumn<ProductoDTO, String> nombreProductoCol;
    @FXML private TableColumn<ProductoDTO, Double> precioProductoCol;
    @FXML private TableColumn<ProductoDTO, String> categoriaProductoCol;

    // --- NUEVOS Elementos FXML para Pedido Actual ---
    @FXML private TableView<DetallePedidoMesaDTO> pedidoActualTableView;
    @FXML private TableColumn<DetallePedidoMesaDTO, String> pedidoNombreCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Integer> pedidoCantidadCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoPrecioCol;
    @FXML private TableColumn<DetallePedidoMesaDTO, Double> pedidoSubtotalCol;
    @FXML private Label totalPedidoLabel; // Para mostrar el total
    @FXML private Button crearPedidoButton; // Botón para enviar

    // --- Servicios ---
    private final MesaService mesaService = new MesaService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoMesaService pedidoMesaService = new PedidoMesaService(); // ⬅️ AÑADIDO

    // --- Estado ---
    private MesaDTO mesaSeleccionada = null;
    private final ObservableList<ProductoDTO> productosData = FXCollections.observableArrayList();
    // ⬇️ Lista Observable para el Pedido Actual ⬇️
    private final ObservableList<DetallePedidoMesaDTO> pedidoActualData = FXCollections.observableArrayList();
    private final ObservableList<CategoriaDTO> categoriasData = FXCollections.observableArrayList(); // ⬅️ AÑADIDO para el diálogo

    @FXML
    public void initialize() {
        infoLabel.setText("Cargando datos iniciales...");
        configurarTablaProductos(); // <-- IMPLEMENTADO
        configurarContenedorMesas(); // (Este estaba vacío pero no es crítico)
        configurarTablaPedidoActual(); // ⬅️ NUEVO: Configurar tabla del pedido
        cargarDatosIniciales();
        configurarSeleccionProducto(); // ⬅️ NUEVO: Añadir listener a tabla productos
        configurarBotonesAdmin(); // ⬅️ AÑADIDO: Añade el botón "Crear Producto"

        crearPedidoButton.setDisable(true); // Deshabilitar botón hasta que haya mesa y productos

        // Listener para seleccionar categoría (sin cambios)
        categoriasListView.getSelectionModel().selectedItemProperty().addListener(
                // ⬇️ ESTA ES LA EXPRESIÓN LAMBDA COMPLETA ⬇️
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        // Acción al seleccionar una categoría (newValue es el CategoriaDTO seleccionado)
                        infoLabel.setText("Categoría seleccionada: " + newValue.getNombre());
                        // Aquí podrías añadir lógica para filtrar la tabla de productos
                        // por newValue.getIdCategoria() si lo deseas.
                    }
                }
                // ⬆️ FIN DE LA EXPRESIÓN LAMBDA ⬆️
        );
    }

    private void configurarContenedorMesas() {
        // (Este método puede quedar vacío por ahora, la lógica está en mostrarMesas)
    }

    // --- IMPLEMENTADO: Configurar Tabla Productos ---
    private void configurarTablaProductos() {
        nombreProductoCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        precioProductoCol.setCellValueFactory(new PropertyValueFactory<>("precio"));
        categoriaProductoCol.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));

        // Asignar la lista observable (vacía al inicio) a la tabla
        productosTableView.setItems(productosData);
    }

    // --- NUEVO: Configurar Tabla Pedido Actual ---
    private void configurarTablaPedidoActual() {
        pedidoNombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        pedidoCantidadCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        pedidoPrecioCol.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        pedidoSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal")); // Asegúrate que DetallePedidoMesaDTO tenga getSubtotal()
        pedidoActualTableView.setItems(pedidoActualData);

        // Placeholder si no hay items
        pedidoActualTableView.setPlaceholder(new Label("Seleccione una mesa y añada productos"));
    }

    // --- NUEVO: Configurar Selección de Producto ---
    private void configurarSeleccionProducto() {
        productosTableView.setOnMouseClicked(event -> {
            // Detectar doble clic
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                ProductoDTO productoSeleccionado = productosTableView.getSelectionModel().getSelectedItem();
                if (productoSeleccionado != null) {
                    handleSeleccionarProducto(productoSeleccionado);
                }
            }
        });
    }


    // --- AÑADIDO: Configurar Botones de Admin ---
    private void configurarBotonesAdmin() {
        // TODO: Añadir lógica para verificar si el usuario es ADMIN
        // Por ahora, el botón se mostrará siempre, pero la API
        // solo funcionará si el token es de un ADMIN.

        Button crearProductoBtn = new Button("Crear Nuevo Producto");
        crearProductoBtn.getStyleClass().addAll("btn", "btn-info"); // Estilo Bootstrap
        crearProductoBtn.setOnAction(e -> handleCrearProducto());
        crearProductoBtn.setMaxWidth(Double.MAX_VALUE); // Que ocupe el ancho

        // Obtener el VBox que contiene la tabla de productos
        try {
            VBox parentVBox = (VBox) productosTableView.getParent();
            if (parentVBox != null) {
                // Añadir el botón después de la etiqueta "Productos:" (índice 1)
                parentVBox.getChildren().add(1, crearProductoBtn);
            }
        } catch (Exception e) {
            System.err.println("Error al intentar añadir el botón 'Crear Producto' a la UI: " + e.getMessage());
        }
    }

    private void cargarDatosIniciales() {
        infoLabel.setText("Cargando datos iniciales..."); // Mensaje inicial
        infoLabel.getStyleClass().setAll("lbl-warning"); // Estilo de carga

        new Thread(() -> {
            List<MesaDTO> mesas = null;
            List<CategoriaDTO> categorias = null;
            List<ProductoDTO> productos = null;
            String errorMessage = null; // Variable para guardar mensaje de error específico
            Exception caughtException = null; // Variable para guardar la excepción

            try {
                System.out.println("Dashboard: Iniciando carga de datos en hilo secundario..."); // DEBUG

                System.out.println("Dashboard: Intentando cargar mesas..."); // DEBUG
                mesas = mesaService.getAllMesas();
                System.out.println("Dashboard: Mesas cargadas OK (" + (mesas != null ? mesas.size() : 0) + " encontradas)."); // DEBUG

                System.out.println("Dashboard: Intentando cargar categorías..."); // DEBUG
                categorias = categoriaService.getAllCategorias();
                System.out.println("Dashboard: Categorías cargadas OK (" + (categorias != null ? categorias.size() : 0) + " encontradas)."); // DEBUG

                System.out.println("Dashboard: Intentando cargar productos..."); // DEBUG
                productos = productoService.getAllProductos();
                System.out.println("Dashboard: Productos cargados OK (" + (productos != null ? productos.size() : 0) + " encontrados)."); // DEBUG

            } catch (HttpClientService.AuthenticationException e) {
                errorMessage = "Error de autenticación: Sesión inválida o expirada.";
                caughtException = e;
                System.err.println("Dashboard: Error de autenticación al cargar datos."); // DEBUG
            } catch (JsonSyntaxException e) { // Captura específica para errores de formato JSON
                errorMessage = "Error al procesar respuesta del servidor (formato JSON inválido).";
                caughtException = e;
                System.err.println("Dashboard: Error de sintaxis JSON."); // DEBUG
            } catch (IOException e) { // Captura errores de red / IO / Status != 2xx
                errorMessage = "Error de conexión con el servidor: " + e.getMessage();
                caughtException = e;
                System.err.println("Dashboard: Error de IO/Red."); // DEBUG
            } catch (InterruptedException e) {
                errorMessage = "Carga de datos interrumpida.";
                caughtException = e;
                Thread.currentThread().interrupt(); // Restablecer estado de interrupción
                System.err.println("Dashboard: Hilo interrumpido."); // DEBUG
            } catch (Exception e) { // Captura cualquier otro error inesperado
                errorMessage = "Error inesperado al cargar datos.";
                caughtException = e;
                System.err.println("Dashboard: Error inesperado general."); // DEBUG
            }

            // --- Actualización de la UI ---
            // Se ejecuta siempre, haya error o no.
            final List<MesaDTO> finalMesas = mesas;
            final List<CategoriaDTO> finalCategorias = categorias;
            final List<ProductoDTO> finalProductos = productos;
            final String finalErrorMessage = errorMessage;
            final Exception finalCaughtException = caughtException;

            Platform.runLater(() -> {
                if (finalErrorMessage == null) {
                    // Éxito: Mostrar todos los datos
                    System.out.println("Dashboard: Actualizando UI con datos cargados."); // DEBUG
                    mostrarMesas(finalMesas); // <-- IMPLEMENTADO
                    mostrarCategorias(finalCategorias); // <-- IMPLEMENTADO
                    mostrarProductos(finalProductos); // <-- IMPLEMENTADO
                    infoLabel.setText("Datos cargados correctamente.");
                    infoLabel.getStyleClass().setAll("lbl-success");
                } else {
                    // Error: Mostrar mensaje de error y limpiar vistas
                    System.out.println("Dashboard: Actualizando UI con mensaje de error: " + finalErrorMessage); // DEBUG
                    infoLabel.setText(finalErrorMessage);
                    infoLabel.getStyleClass().setAll("lbl-danger");
                    // Limpiar vistas para que no se queden en "Cargando..."
                    mesasContainer.getChildren().clear();
                    mesasContainer.getChildren().add(new Label("Error al cargar mesas"));
                    categoriasListView.getItems().clear();
                    categoriasListView.setPlaceholder(new Label("Error al cargar categorías"));
                    productosData.clear();
                    productosTableView.setPlaceholder(new Label("Error al cargar productos"));

                    if (finalCaughtException != null) {
                        // Imprimir el stack trace en la consola para más detalles
                        finalCaughtException.printStackTrace();
                    }
                    if (finalCaughtException instanceof HttpClientService.AuthenticationException) {
                        // TODO: Implementar lógica para cerrar sesión y volver al login
                        System.out.println("Dashboard: Redirigir a Login debido a AuthenticationException.");
                        handleAuthenticationError((HttpClientService.AuthenticationException) finalCaughtException);
                    } else {
                        handleGenericError(finalErrorMessage, finalCaughtException);
                    }
                }
            });

        }).start();
    }

    // --- IMPLEMENTADO: Mostrar Mesas ---
    private void mostrarMesas(List<MesaDTO> mesas) {
        mesasContainer.getChildren().clear(); // Limpiar el "Cargando..."
        if (mesas != null && !mesas.isEmpty()) {
            for (MesaDTO mesa : mesas) {
                Button mesaButton = new Button("Mesa " + mesa.getNumeroMesa());
                mesaButton.setUserData(mesa); // Guardar el DTO completo en el botón
                mesaButton.setPrefSize(100, 80); // Tamaño consistente
                mesaButton.getStyleClass().add("btn"); // Estilo base

                // Aplicar estilo según el estado
                switch (mesa.getEstado()) {
                    case "DISPONIBLE":
                        mesaButton.getStyleClass().add("btn-success"); // Verde
                        break;
                    case "OCUPADA":
                        mesaButton.getStyleClass().add("btn-danger"); // Rojo
                        break;
                    case "RESERVADA":
                        mesaButton.getStyleClass().add("btn-warning"); // Naranja
                        break;
                    default:
                        mesaButton.getStyleClass().add("btn-secondary"); // Gris
                }

                // Acción al hacer clic
                mesaButton.setOnAction(event -> {
                    MesaDTO mesaData = (MesaDTO) ((Button) event.getSource()).getUserData();
                    handleSeleccionarMesa(mesaData);
                });

                mesasContainer.getChildren().add(mesaButton);
            }
        } else {
            mesasContainer.getChildren().add(new Label("No se encontraron mesas."));
        }
    }

    private void handleSeleccionarMesa(MesaDTO mesa) {
        if ("DISPONIBLE".equals(mesa.getEstado())) {
            this.mesaSeleccionada = mesa;
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " seleccionada. Añada productos al pedido.");
            infoLabel.getStyleClass().setAll("lbl-info");
            pedidoActualData.clear(); // Limpiar pedido anterior al seleccionar nueva mesa
            actualizarTotalPedido(); // Poner total a 0.0
            actualizarEstadoCrearPedidoButton(); // Habilitar/deshabilitar botón
            // System.out.println("Mesa seleccionada: " + mesaSeleccionada.getIdMesa()); // Debug
        } else {
            infoLabel.setText("Mesa " + mesa.getNumeroMesa() + " está " + mesa.getEstado() + ".");
            infoLabel.getStyleClass().setAll("lbl-warning");
            this.mesaSeleccionada = null; // Deseleccionar si no está disponible
            pedidoActualData.clear();
            actualizarTotalPedido();
            actualizarEstadoCrearPedidoButton();
        }
    }

    // --- NUEVO: Manejar Selección de Producto ---
    private void handleSeleccionarProducto(ProductoDTO producto) {
        if (mesaSeleccionada == null) {
            mostrarAlerta("Seleccione una mesa", "Por favor, seleccione una mesa DISPONIBLE antes de añadir productos.");
            return;
        }

        // Preguntar cantidad
        TextInputDialog dialog = new TextInputDialog("1"); // Valor por defecto 1
        dialog.setTitle("Añadir Producto");
        dialog.setHeaderText("Añadir '" + producto.getNombre() + "' al pedido");
        dialog.setContentText("Cantidad:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidadStr -> {
            try {
                int cantidad = Integer.parseInt(cantidadStr);
                if (cantidad > 0) {
                    // Crear el DTO del detalle
                    DetallePedidoMesaDTO detalle = new DetallePedidoMesaDTO();
                    detalle.setIdProducto(producto.getIdProducto());
                    detalle.setNombreProducto(producto.getNombre());
                    detalle.setCantidad(cantidad);
                    detalle.setPrecioUnitario(producto.getPrecio());
                    // ❗️ El subtotal se calcula automáticamente en el DTO
                    // detalle.setSubtotal(cantidad * producto.getPrecio());

                    // Añadir (o actualizar si ya existe) a la lista del pedido
                    // (Lógica simple, se puede mejorar para agrupar)
                    pedidoActualData.add(detalle);
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton(); // Habilitar botón si hay items

                } else {
                    mostrarAlerta("Cantidad inválida", "La cantidad debe ser un número positivo.");
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Entrada inválida", "Por favor, ingrese un número válido para la cantidad.");
            }
        });
    }

    // --- AÑADIDO: Lógica para crear un nuevo producto ---
    @FXML
    private void handleCrearProducto() {
        if (categoriasData.isEmpty()) {
            mostrarAlerta("Error", "Las categorías aún no se han cargado. Espere un momento e intente de nuevo.");
            return;
        }

        // 1. Crear el diálogo
        Dialog<ProductoDTO> dialog = new Dialog<>();
        dialog.setTitle("Crear Nuevo Producto");
        dialog.setHeaderText("Ingrese los detalles del nuevo producto (plato).");

        // 2. Añadir botones (OK y Cancelar)
        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);

        // 3. Crear el layout del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre del plato (ej. Lomo Saltado)");
        TextArea descripcionField = new TextArea(); // Usamos TextArea para descripción
        descripcionField.setPromptText("Descripción (ej. Trozos de lomo fino...)");
        descripcionField.setWrapText(true);
        descripcionField.setPrefRowCount(3);
        TextField precioField = new TextField();
        precioField.setPromptText("Precio (ej. 25.50)");
        ComboBox<CategoriaDTO> categoriaComboBox = new ComboBox<>();
        categoriaComboBox.setItems(categoriasData); // Usar la lista observable cargada
        categoriaComboBox.setPromptText("Seleccione una categoría");

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descripcionField, 1, 1);
        grid.add(new Label("Precio:"), 0, 2);
        grid.add(precioField, 1, 2);
        grid.add(new Label("Categoría:"), 0, 3);
        grid.add(categoriaComboBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // 4. Validación (deshabilitar "Crear" si faltan datos)
        Node crearButton = dialog.getDialogPane().lookupButton(crearButtonType);
        crearButton.setDisable(true);

        // Listener para habilitar el botón
        Runnable validador = () -> {
            boolean invalido = nombreField.getText().trim().isEmpty()
                    || precioField.getText().trim().isEmpty()
                    || categoriaComboBox.getValue() == null;
            crearButton.setDisable(invalido);
        };

        nombreField.textProperty().addListener((o, ov, nv) -> validador.run());
        precioField.textProperty().addListener((o, ov, nv) -> validador.run());
        categoriaComboBox.valueProperty().addListener((o, ov, nv) -> validador.run());

        // 5. Convertir el resultado cuando se presiona "Crear"
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == crearButtonType) {
                try {
                    ProductoDTO nuevoProducto = new ProductoDTO();
                    nuevoProducto.setNombre(nombreField.getText().trim());
                    nuevoProducto.setDescripcion(descripcionField.getText().trim());
                    // Validar que el precio sea un número
                    double precio = Double.parseDouble(precioField.getText().trim());
                    if (precio <= 0) throw new NumberFormatException("El precio debe ser positivo");
                    nuevoProducto.setPrecio(precio);
                    nuevoProducto.setIdCategoria(categoriaComboBox.getValue().getIdCategoria());

                    return nuevoProducto;
                } catch (NumberFormatException e) {
                    mostrarAlerta("Datos Inválidos", "El precio debe ser un número positivo (ej. 25.50).");
                    return null; // Evita que el diálogo se cierre
                }
            }
            return null;
        });

        // 6. Mostrar el diálogo y procesar la respuesta
        Optional<ProductoDTO> result = dialog.showAndWait();

        result.ifPresent(productoACrear -> {
            if (productoACrear == null) return; // Ocurrió un error en el convertidor (ej. precio)

            infoLabel.setText("Creando producto '" + productoACrear.getNombre() + "'...");
            infoLabel.getStyleClass().setAll("lbl-warning");

            // 7. Llamar al servicio en un hilo separado
            new Thread(() -> {
                try {
                    ProductoDTO productoCreado = productoService.crearProducto(productoACrear);
                    // Si tiene éxito, recargar la lista de productos
                    Platform.runLater(() -> {
                        infoLabel.setText("Producto '" + productoCreado.getNombre() + "' creado con éxito.");
                        infoLabel.getStyleClass().setAll("lbl-success");
                        cargarDatosIniciales(); // Recarga todo, incluyendo la nueva lista de productos
                    });

                } catch (HttpClientService.AuthenticationException e) {
                    Platform.runLater(() -> {
                        // Esto pasará si un no-admin intenta crear
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

    // --- NUEVO: Actualizar Total del Pedido ---
    private void actualizarTotalPedido() {
        double total = 0.0;
        for (DetallePedidoMesaDTO detalle : pedidoActualData) {
            total += detalle.getSubtotal(); // Asegúrate que getSubtotal() funcione
        }
        totalPedidoLabel.setText(String.format("Total: S/ %.2f", total));
    }

    // --- NUEVO: Actualizar estado del botón Crear Pedido ---
    private void actualizarEstadoCrearPedidoButton() {
        crearPedidoButton.setDisable(mesaSeleccionada == null || pedidoActualData.isEmpty());
    }

    // --- MODIFICADO: Acción para el botón Crear Pedido ---
    @FXML
    private void handleCrearPedido() {
        if (mesaSeleccionada == null || pedidoActualData.isEmpty()) {
            mostrarAlerta("Pedido incompleto", "Debe seleccionar una mesa y añadir al menos un producto.");
            return;
        }

        infoLabel.setText("Creando pedido...");
        infoLabel.getStyleClass().setAll("lbl-warning");
        crearPedidoButton.setDisable(true); // Deshabilitar mientras se envía

        // 1. Construir el PedidoMesaDTO
        PedidoMesaDTO nuevoPedido = new PedidoMesaDTO();
        nuevoPedido.setIdMesa(mesaSeleccionada.getIdMesa());
        // El idMesero lo asigna el backend basado en el token
        nuevoPedido.setEstado("ABIERTO"); // Estado inicial
        nuevoPedido.setDetalles(new ArrayList<>(pedidoActualData)); // Copiar la lista

        // 2. Llamar al servicio (en hilo separado)
        new Thread(() -> {
            try {
                // ⬇️ LLAMADA REAL AL SERVICIO ⬇️
                PedidoMesaDTO pedidoCreado = pedidoMesaService.crearPedido(nuevoPedido);

                Platform.runLater(() -> {
                    infoLabel.setText("Pedido #" + pedidoCreado.getIdPedidoMesa() + " creado exitosamente para Mesa " + mesaSeleccionada.getNumeroMesa());
                    infoLabel.getStyleClass().setAll("lbl-success");
                    // Limpiar estado
                    pedidoActualData.clear();
                    mesaSeleccionada = null;
                    actualizarTotalPedido();
                    actualizarEstadoCrearPedidoButton();
                    // Recargar mesas para ver el estado actualizado
                    cargarDatosIniciales(); // Esto recargará todo, incluyendo la mesa a estado "OCUPADA"
                });
            } catch (HttpClientService.AuthenticationException e) {
                Platform.runLater(() -> {
                    handleAuthenticationError(e);
                    crearPedidoButton.setDisable(false); // Rehabilitar en error
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    handleGenericError("Error al crear el pedido", e);
                    crearPedidoButton.setDisable(false); // Rehabilitar en error
                });
            }
        }).start();
    }

    // --- NUEVO: Método auxiliar para mostrar alertas ---
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }


    // --- IMPLEMENTADO: Mostrar Categorías ---
    private void mostrarCategorias(List<CategoriaDTO> categorias) {
        categoriasData.clear(); // Limpiar datos anteriores
        if (categorias != null && !categorias.isEmpty()) {
            categoriasData.addAll(categorias); // ⬅️ AÑADIDO: Guardar en la lista observable
            categoriasListView.setItems(categoriasData);
            // El toString() de CategoriaDTO (frontend) ya está configurado
            // para mostrar solo el nombre.
        } else {
            // Si la lista está vacía, el placeholder del FXML se mostrará,
            // pero lo actualizamos por si acaso.
            categoriasListView.setPlaceholder(new Label("No se encontraron categorías."));
        }
    }

    // --- IMPLEMENTADO: Mostrar Productos ---
    private void mostrarProductos(List<ProductoDTO> productos) {
        productosData.clear(); // Limpiar datos anteriores
        if (productos != null && !productos.isEmpty()) {
            productosData.addAll(productos);
        }
        // Si la lista 'productosData' está vacía, la tabla mostrará
        // su placeholder. Actualicémoslo por claridad.
        if (productosData.isEmpty()) {
            productosTableView.setPlaceholder(new Label("No se encontraron productos."));
        }
    }

    // --- IMPLEMENTADO: Manejo de Errores ---
    private void handleAuthenticationError(HttpClientService.AuthenticationException e) {
        infoLabel.setText(e.getMessage());
        infoLabel.getStyleClass().setAll("lbl-danger");
        // Aquí deberías tener lógica para redirigir al Login
        // (Esta parte es más compleja y depende de cómo manejes las ventanas)
        System.err.println("Error de autenticación, se debería redirigir al login.");
        mostrarAlerta("Sesión Expirada", "Su sesión ha expirado o no es válida. Por favor, vuelva a iniciar sesión.");
        // (Lógica de cierre de sesión y cambio de ventana iría aquí)
    }

    private void handleGenericError(String message, Exception e) {
        // Usar el mensaje específico si está disponible, si no, uno genérico
        String errorMessage = (message != null && !message.isBlank()) ? message : "Error inesperado.";
        infoLabel.setText(errorMessage);
        infoLabel.getStyleClass().setAll("lbl-danger");
        if (e != null) {
            e.printStackTrace(); // Imprimir el error en la consola para depuración
        }
        mostrarAlerta("Error", errorMessage);
    }
}

