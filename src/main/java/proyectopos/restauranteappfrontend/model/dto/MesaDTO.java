package proyectopos.restauranteappfrontend.model.dto;

public class MesaDTO {
    private Long idMesa;
    private Integer numeroMesa;
    private Integer capacidad;
    private String estado;

    // Campos visuales
    private Double posX;
    private Double posY;
    private Double width;
    private Double height;
    private Double rotation;
    private String forma;
    private String tipo;

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

     public Double getPosX() { return posX; }
    public void setPosX(Double posX) { this.posX = posX; }
    public Double getPosY() { return posY; }
    public void setPosY(Double posY) { this.posY = posY; }
    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    public Double getRotation() { return rotation; }
    public void setRotation(Double rotation) { this.rotation = rotation; }
    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}