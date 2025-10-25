package proyectopos.restauranteappfrontend.services;

import com.google.gson.reflect.TypeToken;
import proyectopos.restauranteappfrontend.model.dto.RolDTO; // Asegúrate de tener esta clase DTO en el frontend

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class RolService {

    private final HttpClientService httpClientService;
    private static final String ROLES_ENDPOINT = "/api/roles";

    public RolService() {
        this.httpClientService = new HttpClientService();
    }

    /**
     * Obtiene la lista de todos los roles disponibles del backend.
     * Requiere rol de ADMIN.
     * @return Una lista de RolDTO.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido o no es ADMIN.
     */
    public List<RolDTO> getAllRoles() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<RolDTO>>() {}.getType();
        return httpClientService.get(ROLES_ENDPOINT, listType);
    }
}