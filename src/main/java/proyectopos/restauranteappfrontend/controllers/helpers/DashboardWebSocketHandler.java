package proyectopos.restauranteappfrontend.controllers.helpers;

import java.util.Map;

import com.google.gson.Gson;

import proyectopos.restauranteappfrontend.controllers.listeners.DashboardUpdateListener;
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;
import proyectopos.restauranteappfrontend.model.dto.WebSocketMessageDTO;

public class DashboardWebSocketHandler {

    private final Gson gson;
    private final DashboardUpdateListener listener;
    // Necesitamos acceso al estado actual de los pedidos para tomar decisiones
    private final Map<Long, String> estadoPedidoCache;

    public DashboardWebSocketHandler(DashboardUpdateListener listener, Map<Long, String> estadoPedidoCache) {
        this.listener = listener;
        this.estadoPedidoCache = estadoPedidoCache;
        this.gson = new Gson();
    }

    public void procesarMensaje(String jsonMessage, MesaDTO mesaSeleccionadaActual) {
        try {
            WebSocketMessageDTO msg = gson.fromJson(jsonMessage, WebSocketMessageDTO.class);

            // 1. Manejo de mensajes de control o nulos
            if (msg == null || msg.getType() == null) {
                if (esMensajeDeControl(jsonMessage)) {
                    listener.onSystemRefreshRequested();
                }
                return;
            }

            // 2. Parseo del Payload
            PedidoMesaDTO pedido = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
            if (pedido == null || pedido.getIdMesa() == null) return;

            // 3. Calcular nuevos estados
            calcularYNotificarEstadoMesa(msg.getType(), pedido);

            // 4. Verificar si afecta a la selección actual
            verificarImpactoEnSeleccion(msg.getType(), pedido, mesaSeleccionadaActual);

        } catch (Exception e) {
            System.err.println("Error procesando WS: " + e.getMessage());
            listener.onSystemRefreshRequested(); // Fallback seguro
        }
    }

    private boolean esMensajeDeControl(String msg) {
        return "LISTO".equals(msg) || "CERRADO".equals(msg) || "NUEVO".equals(msg);
    }

    private void calcularYNotificarEstadoMesa(String tipoEvento, PedidoMesaDTO pedido) {
        String nuevoEstadoBase = "OCUPADA"; // Valor por defecto
        String nuevoEstadoPedido = null;
        boolean limpiarCache = false;

        switch (tipoEvento) {
            case "PEDIDO_CREADO":
            case "PEDIDO_ACTUALIZADO":
                estadoPedidoCache.put(pedido.getIdMesa(), pedido.getEstado());
                break;
            case "PEDIDO_LISTO":
                nuevoEstadoPedido = "LISTO_PARA_ENTREGAR";
                estadoPedidoCache.put(pedido.getIdMesa(), nuevoEstadoPedido);
                break;
            case "PEDIDO_CERRADO":
            case "PEDIDO_CANCELADO":
                nuevoEstadoBase = "DISPONIBLE";
                limpiarCache = true;
                break;
            default:
                return; // Evento desconocido, no hacemos nada
        }

        if (limpiarCache) {
            estadoPedidoCache.remove(pedido.getIdMesa());
        }

        // Notificar al Dashboard solo con los datos puros
        listener.onMesaStatusChanged(pedido.getIdMesa(), nuevoEstadoBase, nuevoEstadoPedido);
    }

    private void verificarImpactoEnSeleccion(String tipoEvento, PedidoMesaDTO pedido, MesaDTO mesaSeleccionada) {
        if (mesaSeleccionada == null || !mesaSeleccionada.getIdMesa().equals(pedido.getIdMesa())) {
            return;
        }

        if ("PEDIDO_CERRADO".equals(tipoEvento) || "PEDIDO_CANCELADO".equals(tipoEvento)) {
            listener.onPedidoActiveClosed("El pedido de esta mesa ha sido cerrado externamente.");
        } else {
            listener.onPedidoActiveUpdated(pedido);
        }
    }
}