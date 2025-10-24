package proyectopos.restauranteappfrontend.model.dto;

import java.util.List;

/**
 * Representa un Pedido de Mesa, tanto para enviar (crear) como para recibir (listar).
 * Esta clase es un espejo del DTO del backend, pero sin las anotaciones de validación.
 */
public class PedidoMesaDTO {

    private Long idPedidoMesa;
    private Long idMesa;
    private Integer numeroMesa;
    private String nombreMesero;
    // Usamos String para la fecha/hora para simplificar la deserialización con Gson
    private String fechaHoraCreacion;
    private String estado;
    private Double total;

    // Esta es la lista de productos que se van a pedir
    private List<DetallePedidoMesaDTO> detalles;

    // Constructor vacío (necesario para Gson)
    public PedidoMesaDTO() {}

    // Getters y Setters
    public Long getIdPedidoMesa() {
        return idPedidoMesa;
    }

    public void setIdPedidoMesa(Long idPedidoMesa) {
        this.idPedidoMesa = idPedidoMesa;
    }

    public Long getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Long idMesa) {
        this.idMesa = idMesa;
    }

    public Integer getNumeroMesa() {
        return numeroMesa;
    }

    public void setNumeroMesa(Integer numeroMesa) {
        this.numeroMesa = numeroMesa;
    }

    public String getNombreMesero() {
        return nombreMesero;
    }

    public void setNombreMesero(String nombreMesero) {
        this.nombreMesero = nombreMesero;
    }

    public String getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public void setFechaHoraCreacion(String fechaHoraCreacion) {
        this.fechaHoraCreacion = fechaHoraCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<DetallePedidoMesaDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedidoMesaDTO> detalles) {
        this.detalles = detalles;
    }
}
