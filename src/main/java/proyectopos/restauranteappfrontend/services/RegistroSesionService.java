package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import proyectopos.restauranteappfrontend.model.dto.RegistroSesionDTO;

public class RegistroSesionService {

    private final HttpClientService httpClientService;
    private static final String ENDPOINT = "/api/admin/sesiones";

    public RegistroSesionService() {
        this.httpClientService = new HttpClientService();
    }

    public List<RegistroSesionDTO> buscarSesiones(String usuario, LocalDate desde, LocalDate hasta) 
            throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        
        StringBuilder url = new StringBuilder(ENDPOINT + "?");
        
        if (usuario != null && !usuario.isBlank()) {
            url.append("usuario=").append(usuario).append("&");
        }
        if (desde != null) {
            url.append("desde=").append(desde.toString()).append("&");
        }
        if (hasta != null) {
            url.append("hasta=").append(hasta.toString()).append("&");
        }

        Type listType = new TypeToken<List<RegistroSesionDTO>>() {}.getType();
        return httpClientService.get(url.toString(), listType);
    }
}