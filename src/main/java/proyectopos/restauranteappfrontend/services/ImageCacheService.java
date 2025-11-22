package proyectopos.restauranteappfrontend.services;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

public class ImageCacheService {

    private static ImageCacheService instance;
    
    // El mapa donde guardamos las fotos: URL -> Objeto Imagen JavaFX
    private final Map<String, Image> imageCache;

    private ImageCacheService() {
        this.imageCache = new HashMap<>();
    }

    public static synchronized ImageCacheService getInstance() {
        if (instance == null) {
            instance = new ImageCacheService();
        }
        return instance;
    }

    /**
     * Obtiene una imagen. Si ya existe en caché, la devuelve de inmediato.
     * Si no, la descarga en segundo plano y la guarda para la próxima.
     */
    public Image getImage(String url) {
        if (url == null || url.isBlank()) return null;

        // 1. Verificar si ya la tenemos en memoria
        if (imageCache.containsKey(url)) {
            return imageCache.get(url);
        }

        // 2. Si no existe, crearla con "backgroundLoading = true" (carga asíncrona)
        // Esto hace que la interfaz NO se congele mientras baja la foto.
        Image nuevaImagen = new Image(url, true);
        
        // 3. Guardarla en el mapa
        imageCache.put(url, nuevaImagen);

        return nuevaImagen;
    }

    /**
     * (Opcional) Para limpiar memoria si el admin cierra sesión o recarga datos pesados.
     */
    public void clearCache() {
        imageCache.clear();
        System.out.println("Caché de imágenes limpiada.");
    }
}