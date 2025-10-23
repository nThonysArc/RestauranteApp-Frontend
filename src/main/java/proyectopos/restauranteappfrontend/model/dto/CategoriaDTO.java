package proyectopos.restauranteappfrontend.model.dto;

public class CategoriaDTO {
    private Long idCategoria;
    private String nombre;
    private Long idCategoriaPadre; // puede ser null

    // Constructor vacío
    public CategoriaDTO() {}

    // Getters y Setters
    public Long getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Long idCategoria) { this.idCategoria = idCategoria; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getIdCategoriaPadre() { return idCategoriaPadre; }
    public void setIdCategoriaPadre(Long idCategoriaPadre) { this.idCategoriaPadre = idCategoriaPadre; }

    // toString para fácil visualización (ej. en ComboBox o ListView)
    @Override
    public String toString() {
        return nombre; // Muestra solo el nombre en listas
    }
}