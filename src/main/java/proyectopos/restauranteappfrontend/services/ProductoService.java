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

    /**
     * Obtiene la lista de todos los productos del backend.
     * @return Una lista de ProductoDTO.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido.
     */
    public List<ProductoDTO> getAllProductos() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Define el tipo de la respuesta esperada (una Lista de ProductoDTO)
        Type listType = new TypeToken<List<ProductoDTO>>() {}.getType();
        return httpClientService.get(PRODUCTOS_ENDPOINT, listType);
    }

    /**
     * Envía un nuevo producto (POST) al backend.
     * Solo los ADMINs pueden hacer esto.
     *
     * @param nuevoProducto El DTO del producto a crear.
     * @return El ProductoDTO guardado, con el ID asignado por el backend.
     * @throws IOException Si hay error de red.
     * @throws InterruptedException Si se interrumpe la llamada.
     * @throws HttpClientService.AuthenticationException Si el token no es válido o no es ADMIN (401/403).
     */
    public ProductoDTO crearProducto(ProductoDTO nuevoProducto) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Usa el método post genérico del HttpClientService
        // 1. Endpoint: "/api/productos"
        // 2. Body: El objeto nuevoProducto (Gson lo convertirá a JSON)
        // 3. Clase de respuesta esperada: ProductoDTO.class
        return httpClientService.post(PRODUCTOS_ENDPOINT, nuevoProducto, ProductoDTO.class);
    }

    // Podrías añadir un método para obtener productos por categoría si tu backend lo soporta
    /*
    public List<ProductoDTO> getProductosByCategoria(Long idCategoria) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<ProductoDTO>>() {}.getType();
        return httpClientService.get(PRODUCTOS_ENDPOINT + "?categoriaId=" + idCategoria, listType); // Asumiendo que el backend soporta este filtro
    }
    */
}

