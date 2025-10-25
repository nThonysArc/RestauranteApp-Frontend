// EmpleadoDTO.java (Frontend)
package proyectopos.restauranteappfrontend.model.dto;

public class EmpleadoDTO {
    private Long idEmpleado;
    private String nombre;
    private String dni;
    private String usuario;
    private String rolNombre; // Para mostrar en tablas, etc.
    private Long idRol;       // Para enviar al crear/actualizar
    private String contrasena; // Solo se usa al enviar para crear/actualizar

    // Constructor vacío
    public EmpleadoDTO() {}

    // Getters y Setters para todos los campos...
    // (Asegúrate de incluirlos)
    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }
    // ... y así para todos los demás campos
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRolNombre() { return rolNombre; }
    public void setRolNombre(String rolNombre) { this.rolNombre = rolNombre; }
    public Long getIdRol() { return idRol; }
    public void setIdRol(Long idRol) { this.idRol = idRol; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}