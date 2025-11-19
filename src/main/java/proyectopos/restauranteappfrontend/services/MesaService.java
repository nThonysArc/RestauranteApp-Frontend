package proyectopos.restauranteappfrontend.services;

import java.io.IOException; // Necesario para listas
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import proyectopos.restauranteappfrontend.model.dto.MesaDTO;

public class MesaService {

    private final HttpClientService httpClientService;
    private static final String MESAS_ENDPOINT = "/api/mesas";

    public MesaService() {
        this.httpClientService = new HttpClientService();
    }

    /**
     * Obtiene la lista de todas las mesas del backend.
     * @return
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido.
     */
    public List<MesaDTO> getAllMesas() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Define el tipo de la respuesta esperada
        Type listType = new TypeToken<List<MesaDTO>>() {}.getType();
        return httpClientService.get(MESAS_ENDPOINT, listType);
    }

    public MesaDTO crearMesa(MesaDTO mesa) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.post(MESAS_ENDPOINT, mesa, MesaDTO.class);
    }

    public MesaDTO actualizarMesa(Long id, MesaDTO mesa) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.put(MESAS_ENDPOINT + "/" + id, mesa, MesaDTO.class);
    }

    public void eliminarMesa(Long id) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // ADVERTENCIA: Esto borrará el historial de pedidos de esa mesa.
        // Se recomienda usar cambiarEstado a BLOQUEADA en su lugar.
        httpClientService.delete(MESAS_ENDPOINT + "/" + id, Void.class);
    }
}