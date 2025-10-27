// DetallePedidoMesaDTO.java (Frontend)
package proyectopos.restauranteappfrontend.model.dto;

// NO más imports de jakarta.validation

public class DetallePedidoMesaDTO {

    private Long idDetallePedidoMesa;
    // SIN @NotNull
    private Long idProducto;
    private String nombreProducto;
    // SIN @NotNull y @Positive
    private Integer cantidad;
    // SIN @NotNull y @Positive
    private Double precioUnitario;
    private Double subtotal; // Campo para el subtotal
    
    // --- ¡¡NUEVO CAMPO AÑADIDO!! ---
    private String estadoDetalle;


    public DetallePedidoMesaDTO() {}

    // Constructor opcional si lo necesitas
    public DetallePedidoMesaDTO(Long idProducto, String nombreProducto, Integer cantidad, Double precioUnitario) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        calcularSubtotal(); // Calcular al crear
        this.estadoDetalle = "PENDIENTE"; // Asumir PENDIENTE si se crea en el frontend
    }

    // Getters y setters (sin cambios en su lógica interna)
    public Long getIdDetallePedidoMesa() { return idDetallePedidoMesa; }
    public void setIdDetallePedidoMesa(Long idDetallePedidoMesa) { this.idDetallePedidoMesa = idDetallePedidoMesa; }

    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
        calcularSubtotal(); // Recalcular si cambia la cantidad
    }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
        calcularSubtotal(); // Recalcular si cambia el precio
    }

    public Double getSubtotal() {
        // Asegurarse de calcular si aún no está calculado
        if (subtotal == null) {
            calcularSubtotal();
        }
        return subtotal;
    }
    // Setter para Gson (aunque lo calculamos internamente)
    public void setSubtotal(Double subtotal){
        this.subtotal = subtotal;
    }

    // --- NUEVO GETTER/SETTER ---
    public String getEstadoDetalle() { return estadoDetalle; }
    public void setEstadoDetalle(String estadoDetalle) { this.estadoDetalle = estadoDetalle; }


    // Método privado para calcular subtotal
    private void calcularSubtotal() {
        if (this.cantidad != null && this.precioUnitario != null && this.cantidad >= 0 && this.precioUnitario >= 0) {
            this.subtotal = this.cantidad * this.precioUnitario;
        } else {
            this.subtotal = 0.0;
        }
    }
}