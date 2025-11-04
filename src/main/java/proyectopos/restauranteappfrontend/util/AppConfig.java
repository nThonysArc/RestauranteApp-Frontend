package proyectopos.restauranteappfrontend.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Clase Singleton para gestionar la configuración de la aplicación
 */
public class AppConfig {

    private static AppConfig instance;
    private final Properties properties;

    private AppConfig() {
        properties = new Properties();
        // Carga el archivo desde la ruta de resources
        String configFileName = "/proyectopos/restauranteappfrontend/config.properties";
        try (InputStream input = AppConfig.class.getResourceAsStream(configFileName)) {
            if (input == null) {
                System.err.println("Error: No se pudo encontrar el archivo " + configFileName);
                // Lanza una excepción para detener la app si no hay config
                throw new RuntimeException("No se pudo encontrar " + configFileName);
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error al cargar " + configFileName, ex);
        }
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /**
     * Obtiene la URL base de la API desde el archivo config.properties.
     * @return La URL, ej. "http://localhost:8080"
     */
    public String getApiBaseUrl() {
        String url = properties.getProperty("api.baseUrl");
        if (url == null || url.isBlank()) {
            throw new RuntimeException("La propiedad 'api.baseUrl' no está definida en config.properties");
        }
        return url;
    }
}