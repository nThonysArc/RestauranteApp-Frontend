// RolDTO.java (Frontend)
package proyectopos.restauranteappfrontend.model.dto;

public class RolDTO {
    private Long idRol;
    private String nombre;

    // Constructor vacío
    public RolDTO() {}

    // Getters y Setters
    public Long getIdRol() { return idRol; }
    public void setIdRol(Long idRol) { this.idRol = idRol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // toString para mostrar en ComboBox
    @Override
    public String toString() {
        return nombre; // Muestra solo el nombre
    }
}