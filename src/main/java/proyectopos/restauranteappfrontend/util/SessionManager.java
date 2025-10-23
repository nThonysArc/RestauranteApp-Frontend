package proyectopos.restauranteappfrontend.util;

/**
 * Singleton simple para almacenar el token JWT después del login.
 * En una aplicación más compleja, esto podría manejar más datos de sesión.
 */
public class SessionManager {

    private static SessionManager instance;
    private String token;

    private SessionManager() {
        // Constructor privado para evitar instanciación externa
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void clearSession() {
        this.token = null;
        // Podrías limpiar otros datos de sesión aquí
    }

    public boolean isAuthenticated() {
        return this.token != null && !this.token.isBlank();
    }
}