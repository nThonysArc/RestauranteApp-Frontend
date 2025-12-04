package proyectopos.restauranteappfrontend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.services.MesaService;
import proyectopos.restauranteappfrontend.util.DraggableMaker;
import proyectopos.restauranteappfrontend.util.ThreadManager;

public class TableManagementController {

    @FXML private Pane restaurantePlano;
    @FXML private VBox panelPropiedades;
    @FXML private TextField propNumeroField;
    @FXML private TextField propCapacidadField;
    @FXML private Slider propWidthSlider;
    @FXML private Slider propHeightSlider;
    @FXML private Slider propRotationSlider;
    @FXML private ComboBox<String> propTipoCombo;
    @FXML private Label statusLabel;

    private final MesaService mesaService = new MesaService();
    private final DraggableMaker draggableMaker = new DraggableMaker();
    
    // Mapa para vincular el nodo visual (JavaFX) con los datos (DTO)
    private final Map<Node, MesaDTO> mapaNodos = new HashMap<>();
    private Node seleccionActual = null;

    @FXML
    public void initialize() {
        propTipoCombo.setItems(FXCollections.observableArrayList("MESA", "OBSTACULO"));
        
        // Listeners para actualizar la forma en tiempo real al mover sliders
        configListenersPropiedades();
        
        cargarPlano();
    }

    private void cargarPlano() {
        ThreadManager.getInstance().execute(() -> {
            try {
                List<MesaDTO> mesas = mesaService.getAllMesas(); // Asegúrate de que este endpoint traiga los nuevos campos
                Platform.runLater(() -> {
                    restaurantePlano.getChildren().clear();
                    mapaNodos.clear();
                    for (MesaDTO mesa : mesas) {
                        crearNodoVisual(mesa);
                    }
                    statusLabel.setText("Plano cargado.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        });
    }

    // --- CREACIÓN DE OBJETOS VISUALES ---

    private void crearNodoVisual(MesaDTO mesa) {
        // 1. Crear la forma base
        Shape forma;
        double w = mesa.getWidth() != null ? mesa.getWidth() : 80;
        double h = mesa.getHeight() != null ? mesa.getHeight() : 80;

        if ("CIRCLE".equals(mesa.getForma())) {
            forma = new Circle(w / 2); // Radio
        } else {
            forma = new Rectangle(w, h);
        }

        // Estilos según tipo
        if ("OBSTACULO".equals(mesa.getTipo())) {
            forma.setFill(Color.DARKGRAY);
            forma.setStroke(Color.BLACK);
        } else {
            forma.setFill(Color.DODGERBLUE);
            forma.setStroke(Color.DARKBLUE);
        }
        
        // 2. Etiqueta (Numero de mesa)
        Text texto = new Text(String.valueOf(mesa.getNumeroMesa()));
        texto.setFill(Color.WHITE);
        texto.setStyle("-fx-font-weight: bold;");
        if ("OBSTACULO".equals(mesa.getTipo())) texto.setText(""); // Paredes sin numero

        // 3. Contenedor (StackPane apila forma + texto)
        StackPane nodo = new StackPane(forma, texto);
        nodo.setPrefSize(w, h);
        
        // Posición y Rotación inicial
        nodo.setTranslateX(mesa.getPosX() != null ? mesa.getPosX() : 50);
        nodo.setTranslateY(mesa.getPosY() != null ? mesa.getPosY() : 50);
        nodo.setRotate(mesa.getRotation() != null ? mesa.getRotation() : 0);
        nodo.setCursor(Cursor.HAND);

        // 4. Hacerlo interactivo
        draggableMaker.makeDraggable(nodo, 
            (n) -> { 
                // Al arrastrar: Actualizar DTO interno (no la BD aun)
                MesaDTO dto = mapaNodos.get(n);
                dto.setPosX(n.getTranslateX());
                dto.setPosY(n.getTranslateY());
            }, 
            (n) -> seleccionarNodo(n) // Al hacer click
        );

        // Guardar en mapa y agregar al pane
        mapaNodos.put(nodo, mesa);
        restaurantePlano.getChildren().add(nodo);
    }

    // --- ACCIONES DE BARRA DE HERRAMIENTAS ---

    @FXML
    private void agregarMesaRectangular() {
        MesaDTO nueva = new MesaDTO();
        nueva.setNumeroMesa(getNextNumeroDisponible());
        nueva.setCapacidad(4);
        nueva.setForma("RECTANGLE");
        nueva.setTipo("MESA");
        nueva.setPosX(50.0); nueva.setPosY(50.0);
        nueva.setWidth(100.0); nueva.setHeight(60.0);
        crearNodoVisual(nueva);
    }

    @FXML
    private void agregarMesaRedonda() {
        MesaDTO nueva = new MesaDTO();
        nueva.setNumeroMesa(getNextNumeroDisponible());
        nueva.setCapacidad(4);
        nueva.setForma("CIRCLE");
        nueva.setTipo("MESA");
        nueva.setPosX(50.0); nueva.setPosY(50.0);
        nueva.setWidth(80.0); nueva.setHeight(80.0);
        crearNodoVisual(nueva);
    }

    @FXML
    private void agregarObstaculo() {
        MesaDTO pared = new MesaDTO();
        pared.setNumeroMesa(0);
        pared.setCapacidad(0);
        pared.setForma("RECTANGLE");
        pared.setTipo("OBSTACULO");
        pared.setPosX(10.0); pared.setPosY(10.0);
        pared.setWidth(200.0); pared.setHeight(20.0); // Una pared larga
        crearNodoVisual(pared);
    }

    // --- LÓGICA DE SELECCIÓN Y EDICIÓN ---

    private void seleccionarNodo(Node nodo) {
        seleccionActual = nodo;
        MesaDTO dto = mapaNodos.get(nodo);
        
        // Habilitar panel
        panelPropiedades.setDisable(false);
        
        // Llenar campos
        propNumeroField.setText(String.valueOf(dto.getNumeroMesa()));
        propCapacidadField.setText(String.valueOf(dto.getCapacidad()));
        propTipoCombo.setValue(dto.getTipo());
        
        propWidthSlider.setValue(dto.getWidth() != null ? dto.getWidth() : 80);
        propHeightSlider.setValue(dto.getHeight() != null ? dto.getHeight() : 80);
        propRotationSlider.setValue(nodo.getRotate());
        
        // Resaltar visualmente (borde rojo temporal)
        nodo.setStyle("-fx-effect: dropshadow(three-pass-box, red, 10, 0, 0, 0);");
        // Resetear estilo de otros... (simplificado para el ejemplo)
    }

    private void configListenersPropiedades() {
        // Ejemplo: Cambiar ancho en tiempo real
        propWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (seleccionActual != null) {
                MesaDTO dto = mapaNodos.get(seleccionActual);
                dto.setWidth(newVal.doubleValue());
                
                // Actualizar visualmente (esto depende de si es Circle o Rectangle)
                StackPane stack = (StackPane) seleccionActual;
                Shape shape = (Shape) stack.getChildren().get(0);
                
                if (shape instanceof Rectangle) {
                    ((Rectangle) shape).setWidth(newVal.doubleValue());
                } else if (shape instanceof Circle) {
                    ((Circle) shape).setRadius(newVal.doubleValue() / 2);
                }
                stack.setPrefWidth(newVal.doubleValue());
            }
        });

        // Rotación
        propRotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (seleccionActual != null) {
                seleccionActual.setRotate(newVal.doubleValue());
                mapaNodos.get(seleccionActual).setRotation(newVal.doubleValue());
            }
        });
        
        // Listener para inputs de texto (número)
        propNumeroField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (seleccionActual != null && !newVal.isEmpty()) {
                try {
                    int num = Integer.parseInt(newVal);
                    MesaDTO dto = mapaNodos.get(seleccionActual);
                    dto.setNumeroMesa(num);
                    // Actualizar texto en el nodo
                    StackPane stack = (StackPane) seleccionActual;
                    Text text = (Text) stack.getChildren().get(1);
                    text.setText("OBSTACULO".equals(dto.getTipo()) ? "" : newVal);
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    @FXML
    private void eliminarSeleccion() {
        if (seleccionActual != null) {
            MesaDTO dto = mapaNodos.get(seleccionActual);
            // Si tiene ID, deberíamos marcarlo para borrar en BD, pero por ahora solo lo quitamos visualmente
            // y luego en guardar se procesará (Ojo: Para borrar real necesitas un endpoint de delete)
            restaurantePlano.getChildren().remove(seleccionActual);
            mapaNodos.remove(seleccionActual);
            panelPropiedades.setDisable(true);
            seleccionActual = null;
        }
    }

    @FXML
    private void guardarCambios() {
        statusLabel.setText("Guardando...");
        ThreadManager.getInstance().execute(() -> {
            try {
                // Guardar cada mesa una por una
                for (MesaDTO dto : mapaNodos.values()) {
                    if (dto.getIdMesa() == null) {
                        mesaService.crearMesa(dto); // Crear nuevas
                    } else {
                        mesaService.actualizarMesa(dto.getIdMesa(), dto); // Actualizar existentes
                    }
                }
                Platform.runLater(() -> statusLabel.setText("¡Plano guardado exitosamente!"));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error al guardar: " + e.getMessage()));
            }
        });
    }

    private int getNextNumeroDisponible() {
        return mapaNodos.values().stream()
                .mapToInt(MesaDTO::getNumeroMesa)
                .max().orElse(0) + 1;
    }
}