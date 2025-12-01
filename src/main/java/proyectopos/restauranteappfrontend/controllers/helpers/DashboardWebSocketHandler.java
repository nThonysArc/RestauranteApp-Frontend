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
    private final Map<Long, String> estadoPedidoCache;

    public DashboardWebSocketHandler(DashboardUpdateListener listener, Map<Long, String> estadoPedidoCache) {
        this.listener = listener;
        this.estadoPedidoCache = estadoPedidoCache;
        this.gson = new Gson();
    }

    public void procesarMensaje(String jsonMessage, MesaDTO mesaSeleccionadaActual) {
        try {
            WebSocketMessageDTO msg = gson.fromJson(jsonMessage, WebSocketMessageDTO.class);

            if (msg == null || msg.getType() == null) {
                if (esMensajeDeControl(jsonMessage)) {
                    listener.onSystemRefreshRequested();
                }
                return;
            }

            PedidoMesaDTO pedido = gson.fromJson(msg.getPayload(), PedidoMesaDTO.class);
            if (pedido == null || pedido.getIdMesa() == null) return;

            calcularYNotificarEstadoMesa(msg.getType(), pedido);
            verificarImpactoEnSeleccion(msg.getType(), pedido, mesaSeleccionadaActual);

        } catch (Exception e) {
            System.err.println("Error procesando WS: " + e.getMessage());
            listener.onSystemRefreshRequested(); 
        }
    }

    private boolean esMensajeDeControl(String msg) {
        return "LISTO".equals(msg) || "CERRADO".equals(msg) || "NUEVO".equals(msg);
    }

    private void calcularYNotificarEstadoMesa(String tipoEvento, PedidoMesaDTO pedido) {
        String nuevoEstadoBase = "OCUPADA"; 
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
                return; 
        }

        if (limpiarCache) {
            estadoPedidoCache.remove(pedido.getIdMesa());
        }

        listener.onMesaStatusChanged(pedido.getIdMesa(), nuevoEstadoBase, nuevoEstadoPedido);
    }

    private void verificarImpactoEnSeleccion(String tipoEvento, PedidoMesaDTO pedido, MesaDTO mesaSeleccionada) {
        if (mesaSeleccionada == null || !mesaSeleccionada.getIdMesa().equals(pedido.getIdMesa())) {
            return;
        }

        if ("PEDIDO_CERRADO".equals(tipoEvento) || "PEDIDO_CANCELADO".equals(tipoEvento)) {
            // --- CAMBIO: Mensaje corto y profesional ---
            listener.onPedidoActiveClosed("Pedido finalizado"); 
        } else {
            listener.onPedidoActiveUpdated(pedido);
        }
    }
}