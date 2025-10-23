package proyectopos.restauranteappfrontend.model.dto;

// No necesitamos validaciones aquí, solo los campos y getters/setters
// Puedes usar Lombok si lo prefieres para simplificar
public class MesaDTO {
    private Long idMesa;
    private Integer numeroMesa;
    private Integer capacidad;
    private String estado; // Podrías usar un Enum si quieres más seguridad de tipos

    // Constructor vacío (necesario para Gson/Jackson)
    public MesaDTO() {}

    // Getters y Setters
    public Long getIdMesa() { return idMesa; }
    public void setIdMesa(Long idMesa) { this.idMesa = idMesa; }
    public Integer getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(Integer numeroMesa) { this.numeroMesa = numeroMesa; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // (Opcional) toString para depuración
    @Override
    public String toString() {
        return "MesaDTO{" +
                "idMesa=" + idMesa +
                ", numeroMesa=" + numeroMesa +
                ", capacidad=" + capacidad +
                ", estado='" + estado + '\'' +
                '}';
    }
}