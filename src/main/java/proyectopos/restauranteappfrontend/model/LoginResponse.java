package proyectopos.restauranteappfrontend.model;

/**
 * Representa la respuesta JSON del backend al hacer login exitoso.
 * Contiene el token JWT.
 */
public class LoginResponse {
    private String token;

    // Getters y Setters (Necesarios para Gson)
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}