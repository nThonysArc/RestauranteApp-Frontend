package proyectopos.restauranteappfrontend.controllers;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;

public class MesaTileController {

    @FXML private Button mesaButton;
    @FXML private Label numeroMesaLabel;
    @FXML private Label estadoLabel;

    private MesaDTO mesa;

    public void setMesaData(MesaDTO mesa, Consumer<MesaDTO> onSelectAction) {
        this.mesa = mesa;
        
        // 1. Datos básicos
        numeroMesaLabel.setText(String.valueOf(mesa.getNumeroMesa()));
        
        // 2. Aplicar estilos visuales según el estado
        actualizarEstadoVisual(mesa.getEstado(), null);

        // 3. Configurar acción al hacer clic
        if ("DISPONIBLE".equals(mesa.getEstado()) || "OCUPADA".equals(mesa.getEstado())) {
            mesaButton.setDisable(false);
            mesaButton.setOnAction(e -> onSelectAction.accept(mesa));
        } else {
            mesaButton.setDisable(true); // Reservada o Bloqueada
        }
    }

    /**
     * Actualiza el color y texto del botón sin tener que recargar todo el componente.
     * Se usa cuando llega un evento de WebSocket.
     */
    public void actualizarEstadoVisual(String estadoBase, String estadoPedido) {
        // Limpiar estilos previos
        mesaButton.getStyleClass().removeAll("mesa-libre", "mesa-ocupada", "mesa-pagando", "mesa-reservada", "btn-secondary");
        
        // Lógica de estilos (Extraída del Dashboard original)
        switch (estadoBase) {
            case "DISPONIBLE":
                mesaButton.getStyleClass().add("mesa-libre");
                estadoLabel.setText("Libre");
                numeroMesaLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                break;
            case "OCUPADA":
                if ("LISTO_PARA_ENTREGAR".equalsIgnoreCase(estadoPedido)) {
                    mesaButton.getStyleClass().add("mesa-pagando");
                    estadoLabel.setText("¡LISTO!");
                    // Color especial para indicar que hay comida lista (Amber oscuro)
                    numeroMesaLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #78350f;"); 
                } else {
                    mesaButton.getStyleClass().add("mesa-ocupada");
                    estadoLabel.setText("Ocupada");
                    numeroMesaLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                }
                break;
            case "RESERVADA":
                mesaButton.getStyleClass().add("mesa-reservada");
                estadoLabel.setText("Rsrv.");
                break;
            default:
                mesaButton.getStyleClass().add("btn-secondary");
                estadoLabel.setText(estadoBase);
                break;
        }
        
        // Actualizar el DTO interno por si se necesita después
        this.mesa.setEstado(estadoBase);
    }

    public MesaDTO getMesa() {
        return mesa;
    }
    
    public Button getRootNode() {
        return mesaButton;
    }
}