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

    // --- ¡¡NUEVO MÉTODO AÑADIDO!! ---
    /**
     * Envía una actualización (PUT) a un pedido existente en el backend.
     * IMPORTANTE: El DTO solo debe contener los NUEVOS items.
     */
    public PedidoMesaDTO actualizarPedido(Long pedidoId, PedidoMesaDTO pedidoConNuevosItems) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/" + pedidoId;
        return httpClientService.put(endpoint, pedidoConNuevosItems, PedidoMesaDTO.class);
    }

    /**
     * Obtiene la lista de todos los pedidos.
     */
    public List<PedidoMesaDTO> getAllPedidos() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<PedidoMesaDTO>>() {}.getType();
        return httpClientService.get(PEDIDOS_ENDPOINT, listType);
    }

    // --- ¡¡NUEVO MÉTODO AÑADIDO!! ---
    /**
     * Obtiene el pedido activo de una mesa específica.
     */
    public PedidoMesaDTO getPedidoActivoPorMesa(Long mesaId) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/mesa/" + mesaId + "/activo";
        try {
            return httpClientService.get(endpoint, PedidoMesaDTO.class);
        } catch (IOException e) {
            // Si da un 404 (Not Found), HttpClientService lanza IOException.
            // Lo interpretamos como que no hay pedido activo.
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return null; // No hay pedido activo, no es un error.
            }
            throw e; // Lanzar si es otro tipo de error (ej. 500, 401)
        }
    }


    /**
     * (DEPRECADO POR COCINA) Solicita al backend cambiar el estado de un pedido.
     * @param pedidoId El ID del pedido a modificar.
     * @param nuevoEstado El nuevo estado deseado (ej. "LISTO_PARA_ENTREGAR").
     * @return El PedidoMesaDTO actualizado devuelto por el backend.
     */
    public PedidoMesaDTO cambiarEstadoPedido(Long pedidoId, String nuevoEstado) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/" + pedidoId + "/estado/" + nuevoEstado;
        return httpClientService.put(endpoint, null, PedidoMesaDTO.class); // Enviamos null como cuerpo
    }

    // --- ¡¡NUEVO MÉTODO PARA COCINA!! ---
    /**
     * Solicita al backend marcar todos los items PENDIENTES como LISTOS.
     * @param pedidoId El ID del pedido a modificar.
     * @return El PedidoMesaDTO actualizado.
     */
    public PedidoMesaDTO marcarPendientesComoListos(Long pedidoId) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        String endpoint = PEDIDOS_ENDPOINT + "/" + pedidoId + "/marcarListos";
        return httpClientService.put(endpoint, null, PedidoMesaDTO.class);
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