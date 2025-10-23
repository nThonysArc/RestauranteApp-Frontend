package proyectopos.restauranteappfrontend.services;

import com.google.gson.reflect.TypeToken;
import proyectopos.restauranteappfrontend.model.dto.CategoriaDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class CategoriaService {

    private final HttpClientService httpClientService;
    private static final String CATEGORIAS_ENDPOINT = "/api/categorias";

    public CategoriaService() {
        this.httpClientService = new HttpClientService();
    }

    public List<CategoriaDTO> getAllCategorias() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<CategoriaDTO>>() {}.getType();
        return httpClientService.get(CATEGORIAS_ENDPOINT, listType);
    }
}