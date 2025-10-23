package proyectopos.restauranteappfrontend.services;

import com.google.gson.reflect.TypeToken;
import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class ProductoService {

    private final HttpClientService httpClientService;
    private static final String PRODUCTOS_ENDPOINT = "/api/productos";

    public ProductoService() {
        this.httpClientService = new HttpClientService();
    }

    public List<ProductoDTO> getAllProductos() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<ProductoDTO>>() {}.getType();
        return httpClientService.get(PRODUCTOS_ENDPOINT, listType);
    }

    // Podrías añadir un método para obtener productos por categoría si tu backend lo soporta
    /*
    public List<ProductoDTO> getProductosByCategoria(Long idCategoria) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<ProductoDTO>>() {}.getType();
        return httpClientService.get(PRODUCTOS_ENDPOINT + "?categoriaId=" + idCategoria, listType); // Asumiendo que el backend soporta este filtro
    }
    */
}