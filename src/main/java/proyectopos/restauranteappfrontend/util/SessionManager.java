package proyectopos.restauranteappfrontend.util;

// <-- AÑADIR IMPORT
import proyectopos.restauranteappfrontend.model.LoginResponse; 

public class SessionManager {

    private static SessionManager instance;
    
    // <-- MODIFICADO: Almacenamos el objeto completo, no solo el token
    private LoginResponse userData;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // <-- MODIFICADO: Método para guardar la respuesta completa
    public void setLoginResponse(LoginResponse response) {
        this.userData = response;
    }
    
    // <-- MODIFICADO: Getter para el objeto completo
    public LoginResponse getUserData() {
        return this.userData;
    }

    // <-- MODIFICADO: Getters específicos para los datos
    
    public String getToken() {
        if (userData != null) {
            return userData.getToken();
        }
        return null;
    }
    
    public String getRole() {
        if (userData != null) {
            return userData.getRol();
        }
        return null;
    }
    
    public String getNombre() {
        if (userData != null) {
            return userData.getNombre();
        }
        return "Usuario Desconocido";
    }

    public void clearSession() {
        this.userData = null; // <-- MODIFICADO
    }

    public boolean isAuthenticated() {
        // La sesión es válida si tenemos datos y un token
        return this.userData != null && this.userData.getToken() != null && !this.userData.getToken().isBlank();
    }
}