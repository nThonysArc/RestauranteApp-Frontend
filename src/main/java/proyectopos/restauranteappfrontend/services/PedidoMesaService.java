package proyectopos.restauranteappfrontend.services;

import com.google.gson.reflect.TypeToken;
import proyectopos.restauranteappfrontend.model.dto.PedidoMesaDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class PedidoMesaService {

    private final HttpClientService httpClientService;
    private static final String PEDIDOS_ENDPOINT = "/api/pedidosMesa"; // Endpoint del backend

    public PedidoMesaService() {
        this.httpClientService = new HttpClientService();
    }

    /**
     * Envía un nuevo pedido (POST) al backend.
     *
     * @param nuevoPedido El DTO del pedido a crear.
     * @return El PedidoMesaDTO guardado, con el ID y totales asignados por el backend.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido.
     */
    public PedidoMesaDTO crearPedido(PedidoMesaDTO nuevoPedido) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Usa el método post genérico del HttpClientService
        // 1. Endpoint: "/api/pedidosMesa"
        // 2. Body: El objeto nuevoPedido (Gson lo convertirá a JSON)
        // 3. Clase de respuesta esperada: PedidoMesaDTO.class
        return httpClientService.post(PEDIDOS_ENDPOINT, nuevoPedido, PedidoMesaDTO.class);
    }

    /**
     * (Opcional) Obtiene la lista de todos los pedidos.
     * @return Una lista de PedidoMesaDTO.
     */
    public List<PedidoMesaDTO> getAllPedidos() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<PedidoMesaDTO>>() {}.getType();
        return httpClientService.get(PEDIDOS_ENDPOINT, listType);
    }

    // --- (Aquí se podrían añadir métodos para listar, cerrar o eliminar pedidos) ---

}
