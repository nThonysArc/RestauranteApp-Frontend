package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;

public class PedidoMesaService {

    private final HttpClientService httpClientService;
    private static final String PEDIDOS_ENDPOINT = "/api/pedidosMesa";

    public PedidoMesaService() {
        this.httpClientService = new HttpClientService();
    }

    /**
     * Envía un nuevo pedido (POST) al backend.
     */
    public PedidoMesaDTO crearPedido(PedidoMesaDTO nuevoPedido) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.post(PEDIDOS_ENDPOINT, nuevoPedido, PedidoMesaDTO.class);
    }

    /**
     * Obtiene la lista de todos los pedidos.
     */
    public List<PedidoMesaDTO> getAllPedidos() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<PedidoMesaDTO>>() {}.getType();
        return httpClientService.get(PEDIDOS_ENDPOINT, listType);
    }

    /**
     * Solicita al backend cambiar el estado de un pedido.
     * @param pedidoId El ID del pedido a modificar.
     * @param nuevoEstado El nuevo estado deseado (ej. "LISTO_PARA_ENTREGAR").
     * @return El PedidoMesaDTO actualizado devuelto por el backend.
     */
    public PedidoMesaDTO cambiarEstadoPedido(Long pedidoId, String nuevoEstado) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/" + pedidoId + "/estado/" + nuevoEstado;
        return httpClientService.put(endpoint, null, PedidoMesaDTO.class); // Enviamos null como cuerpo
    }

    // --- ¡¡NUEVO MÉTODO: Cerrar Pedido!! ---
    /**
     * Solicita al backend cerrar un pedido específico.
     * Esto usualmente cambia el estado a CERRADO y libera la mesa.
     * @param pedidoId El ID del pedido a cerrar.
     * @return El PedidoMesaDTO actualizado con el estado CERRADO.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido o no tiene permisos.
     */
    public PedidoMesaDTO cerrarPedido(Long pedidoId) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/" + pedidoId + "/cerrar";
        // La llamada PUT a este endpoint específico no requiere cuerpo
        return httpClientService.put(endpoint, null, PedidoMesaDTO.class);
    }
    // --- FIN NUEVO MÉTODO ---

}