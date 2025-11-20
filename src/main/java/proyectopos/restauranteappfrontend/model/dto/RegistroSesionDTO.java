package proyectopos.restauranteappfrontend.model.dto;

public class RegistroSesionDTO {
    private Long id;
    private String nombreEmpleado;
    private String usuario;
    private String rol;
    private String fechaLogin;
    private String ipAddress;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getFechaLogin() { return fechaLogin; }
    public void setFechaLogin(String fechaLogin) { this.fechaLogin = fechaLogin; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}