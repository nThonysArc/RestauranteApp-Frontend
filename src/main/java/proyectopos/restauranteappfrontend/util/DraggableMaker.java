package proyectopos.restauranteappfrontend.util;
import java.util.function.Consumer;

import javafx.scene.Node;

//Utilidad para hacer que cualquier objeto sea arrastrable en la interfaz gráfica

public class DraggableMaker {
    private double mouseAnchorX;
    private double mouseAnchorY;

    // Hace que un nodo sea arrastrable.
    // onDragAction: Se ejecuta cada vez que se mueve (útil para actualizar coordenadas en tiempo real)
    // onClickAction: Se ejecuta al hacer click (para seleccionarlo)
    public void makeDraggable(Node node, Consumer<Node> onDragAction, Consumer<Node> onClickAction) {
        
        node.setOnMousePressed(mouseEvent -> {
            mouseAnchorX = mouseEvent.getSceneX() - node.getTranslateX();
            mouseAnchorY = mouseEvent.getSceneY() - node.getTranslateY();
            onClickAction.accept(node); 
            mouseEvent.consume();
        });

        node.setOnMouseDragged(mouseEvent -> {
            double newX = mouseEvent.getSceneX() - mouseAnchorX;
            double newY = mouseEvent.getSceneY() - mouseAnchorY;
            // Grid Snapping: Mover de 10 en 10 pixeles para alinear fácil
            newX = Math.round(newX / 10) * 10;
            newY = Math.round(newY / 10) * 10;

            node.setTranslateX(newX);
            node.setTranslateY(newY);
            
            if (onDragAction != null) {
                onDragAction.accept(node);
            }
        });
    }
}
