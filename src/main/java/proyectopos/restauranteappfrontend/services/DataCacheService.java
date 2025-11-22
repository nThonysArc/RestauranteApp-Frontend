package proyectopos.restauranteappfrontend.services;

import java.util.ArrayList;
import java.util.List;

import proyectopos.restauranteappfrontend.model.dto.ProductoDTO;

/**
 * Servicio Singleton para mantener datos en memoria RAM
 * y evitar llamadas repetitivas al backend.
 */
public class DataCacheService {

    private static DataCacheService instance;

    // Aquí guardamos la lista de productos para no pedirla siempre
    private List<ProductoDTO> productosCache;

    private DataCacheService() {
    }

    public static synchronized DataCacheService getInstance() {
        if (instance == null) {
            instance = new DataCacheService();
        }
        return instance;
    }

    /**
     * Obtiene la lista de productos guardada en memoria.
     * @return La lista de productos o null si aún no se ha cargado.
     */
    public List<ProductoDTO> getProductos() {
        return productosCache;
    }

    /**
     * Guarda la lista de productos en memoria.
     * @param productos La lista traída del backend.
     */
    public void setProductos(List<ProductoDTO> productos) {
        // Guardamos una copia para mayor seguridad
        this.productosCache = new ArrayList<>(productos);
    }

    /**
     * Borra el caché. Útil cuando se crea/edita un producto o se cierra sesión.
     */
    public void limpiarCache() {
        this.productosCache = null;
        System.out.println("Caché de datos limpiado.");
    }
}