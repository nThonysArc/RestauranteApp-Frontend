package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import proyectopos.restauranteappfrontend.model.dto.EmpleadoDTO;

public class EmpleadoService {

    private final HttpClientService httpClientService;
    private static final String EMPLEADOS_ENDPOINT = "/api/empleados";

    public EmpleadoService() {
        this.httpClientService = new HttpClientService();
    }

    /**
     * Crea un nuevo empleado en el backend.
     * Requiere rol de ADMIN.
     * @param nuevoEmpleado El DTO del empleado a crear (con nombre, dni, usuario, contraseña, idRol).
     * @return El EmpleadoDTO creado (con ID asignado).
     */
    public EmpleadoDTO crearEmpleado(EmpleadoDTO nuevoEmpleado) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        return httpClientService.post(EMPLEADOS_ENDPOINT, nuevoEmpleado, EmpleadoDTO.class);
    }

    /**
     * Obtiene la lista de todos los empleados (Solo ADMIN).
     * @return Lista de EmpleadoDTO.
     */
    public List<EmpleadoDTO> getAllEmpleados() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<EmpleadoDTO>>() {}.getType();
        return httpClientService.get(EMPLEADOS_ENDPOINT, listType);
    }

    // --- NUEVO MÉTODO: Actualizar Empleado ---
    /**
     * Actualiza un empleado existente en el backend.
     * Requiere rol de ADMIN.
     * @param id El ID del empleado a actualizar.
     * @param empleadoActualizado El DTO con los datos actualizados. La contraseña es opcional; si se incluye, se actualizará.
     * @return El EmpleadoDTO actualizado.
     */
    public EmpleadoDTO actualizarEmpleado(Long id, EmpleadoDTO empleadoActualizado) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // Asegúrate de que el DTO enviado NO tenga el ID en el cuerpo, ya que va en la URL
        // empleadoActualizado.setIdEmpleado(null); // Opcional, depende de cómo lo maneje tu backend
        return httpClientService.put(EMPLEADOS_ENDPOINT + "/" + id, empleadoActualizado, EmpleadoDTO.class);
    }
    // --- FIN NUEVO MÉTODO ---

    // --- NUEVO MÉTODO: Eliminar Empleado ---
    /**
     * Elimina un empleado del backend.
     * Requiere rol de ADMIN.
     * @param id El ID del empleado a eliminar.
     */
    public void eliminarEmpleado(Long id) throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        // HttpClientService.delete devuelve null para respuestas 204 (No Content)
        httpClientService.delete(EMPLEADOS_ENDPOINT + "/" + id, Void.class); // Esperamos Void o Object.class
    }
    // --- FIN NUEVO MÉTODO ---

}