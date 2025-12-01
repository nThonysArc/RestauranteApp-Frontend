package proyectopos.restauranteappfrontend.model.dto;

import java.util.List;

public class PedidoWebDTO {
    private Long idPedidoWeb;
    private String nombreCliente;
    private String fechaHora; // Viene como String del backend
    private String estado;
    private String direccionEntrega;
    private String telefonoContacto;
    private Double total;
    private List<DetallePedidoWebDTO> detalles;

    // Getters y Setters
    public Long getIdPedidoWeb() { return idPedidoWeb; }
    public void setIdPedidoWeb(Long idPedidoWeb) { this.idPedidoWeb = idPedidoWeb; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public List<DetallePedidoWebDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedidoWebDTO> detalles) { this.detalles = detalles; }
}