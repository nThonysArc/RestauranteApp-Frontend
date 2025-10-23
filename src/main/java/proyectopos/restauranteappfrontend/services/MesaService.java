package proyectopos.restauranteappfrontend.services;

import com.google.gson.reflect.TypeToken; // Necesario para listas
import proyectopos.restauranteappfrontend.model.dto.MesaDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class MesaService {

    private final HttpClientService httpClientService;
    private static final String MESAS_ENDPOINT = "/api/mesas";

    public MesaService() {
        this.httpClientService = new HttpClientService(); // O inyéctalo si usas un framework DI
    }

    /**
     * Obtiene la lista de todas las mesas del backend.
     * @return Una lista de MesaDTO.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido.
     */
    public List<MesaDTO> getAllMesas() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Define el tipo de la respuesta esperada (una Lista de MesaDTO)
        Type listType = new TypeToken<List<MesaDTO>>() {}.getType();
        return httpClientService.get(MESAS_ENDPOINT, listType);
    }

    // --- (Añadir métodos para getMesaById, createMesa, updateMesa, deleteMesa si son necesarios) ---
    // Ejemplo:
    /*
    public MesaDTO createMesa(MesaDTO nuevaMesa) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.post(MESAS_ENDPOINT, nuevaMesa, MesaDTO.class);
    }
    */
}