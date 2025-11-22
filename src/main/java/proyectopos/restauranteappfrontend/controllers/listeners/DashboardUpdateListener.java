package proyectopos.restauranteappfrontend.controllers.listeners;

import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;

public interface DashboardUpdateListener {
    // Cuando hay que recargar todo (ej. conexión inicial o error)
    void onSystemRefreshRequested();
    
    // Cuando cambia el estado visual de una mesa (ej. ocupada -> pagando)
    void onMesaStatusChanged(Long idMesa, String nuevoEstadoBase, String nuevoEstadoPedido);
    
    // Cuando el pedido que se está viendo en pantalla cambia
    void onPedidoActiveUpdated(PedidoMesaDTO pedido);
    
    // Cuando el pedido activo se cierra remotamente
    void onPedidoActiveClosed(String mensaje);
}