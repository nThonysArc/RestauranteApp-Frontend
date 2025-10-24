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

    // --- AÑADIDO: Método para crear una categoría ---
    public CategoriaDTO crearCategoria(CategoriaDTO nuevaCategoria) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.post(CATEGORIAS_ENDPOINT, nuevaCategoria, CategoriaDTO.class);
    }

    // --- AÑADIDO: Método para actualizar una categoría ---
    public CategoriaDTO actualizarCategoria(Long id, CategoriaDTO categoria) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.put(CATEGORIAS_ENDPOINT + "/" + id, categoria, CategoriaDTO.class);
    }

    // --- AÑADIDO: Método para eliminar una categoría ---
    // Nota: El backend (CategoriaController) devuelve ResponseEntity<Void> (204)
    public void eliminarCategoria(Long id) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Hacemos la llamada, pero esperamos un tipo 'Object.class'
        // El HttpClientService modificado devolverá 'null' para respuestas 204
        httpClientService.delete(CATEGORIAS_ENDPOINT + "/" + id, Object.class);
    }
}