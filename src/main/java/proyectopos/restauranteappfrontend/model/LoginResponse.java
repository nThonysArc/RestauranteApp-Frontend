package proyectopos.restauranteappfrontend.model;

/**
 * Representa la respuesta JSON del backend al hacer login exitoso.
 * Contiene el token JWT y los datos del usuario.
 */
public class LoginResponse {
    
    // --- CAMPOS ACTUALIZADOS ---
    private String token;
    private Long id;
    private String nombre;
    private String rol;

    // Getters y Setters (Necesarios para Gson)
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}