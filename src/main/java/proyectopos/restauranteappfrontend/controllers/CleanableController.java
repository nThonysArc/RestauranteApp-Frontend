package proyectopos.restauranteappfrontend.controllers;

/**
 * Interfaz para controladores que necesitan realizar acciones de limpieza
 * (ej. detener Timelines, cerrar conexiones) cuando se navega fuera de su vista.
 */
public interface CleanableController {
    /**
     * Método llamado por el MainController antes de reemplazar la vista
     * asociada a este controlador.
     */
    void cleanup();
}