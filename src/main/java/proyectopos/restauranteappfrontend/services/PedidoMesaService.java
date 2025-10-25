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

    // --- NUEVO MÉTODO: Cambiar Estado del Pedido ---
    /**
     * Solicita al backend cambiar el estado de un pedido.
     * @param pedidoId El ID del pedido a modificar.
     * @param nuevoEstado El nuevo estado deseado (ej. "LISTO_PARA_ENTREGAR").
     * @return El PedidoMesaDTO actualizado devuelto por el backend.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido o no tiene permisos.
     */
    public PedidoMesaDTO cambiarEstadoPedido(Long pedidoId, String nuevoEstado) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/" + pedidoId + "/estado/" + nuevoEstado;
        // La llamada PUT no necesita cuerpo en este caso, solo la URL
        return httpClientService.put(endpoint, null, PedidoMesaDTO.class); // Enviamos null como cuerpo
    }
    // --- FIN NUEVO MÉTODO ---


    // --- (Aquí se podrían añadir métodos para cerrar o eliminar pedidos si no los tienes ya) ---

}