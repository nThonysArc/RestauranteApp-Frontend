package proyectopos.restauranteappfrontend.model;

/**
 * Clase de modelo utilizada para enviar credenciales de usuario (username y password)
 * al endpoint de autenticación del backend.
 * Gson utilizará esta clase para serializar el objeto a JSON.
 */
public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters y Setters (Necesarios para que Gson acceda a los campos, aunque no se usen explícitamente)

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
