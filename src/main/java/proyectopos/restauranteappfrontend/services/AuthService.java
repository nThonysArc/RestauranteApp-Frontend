package proyectopos.restauranteappfrontend.services;

import com.google.gson.Gson;
import proyectopos.restauranteappfrontend.model.LoginRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Maneja la comunicación con el endpoint de autenticación del backend.
 */
public class AuthService {

    // URL base de tu backend (Ajustar si es diferente)
    private static final String BASE_URL = "http://localhost:8080";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/api/auth/login";

    private final HttpClient httpClient;
    private final Gson gson;

    public AuthService() {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        this.gson = new Gson();
    }

    /**
     * Realiza la llamada HTTP POST al backend para intentar iniciar sesión.
     * * @param username Nombre de usuario.
     * @param password Contraseña.
     * @return true si el backend devuelve un código 200 (OK), false en caso contrario.
     * @throws IOException Si hay un error de red o de I/O.
     * @throws InterruptedException Si la operación es interrumpida.
     */
    public boolean authenticate(String username, String password) throws IOException, InterruptedException {

        // 1. Crear el objeto de petición
        LoginRequest loginRequest = new LoginRequest(username, password);

        // 2. Serializar el objeto a JSON
        String jsonPayload = gson.toJson(loginRequest);

        // 3. Construir la petición HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_ENDPOINT))
                .header("Content-Type", "application/json") // Indica que estamos enviando JSON
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // 4. Enviar la petición y obtener la respuesta
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 5. Verificar el código de estado (200 OK -> éxito)
        // Nota: En un sistema real, un 200 contendría un JWT o un token de sesión.
        // Por ahora, solo verificamos el éxito de la conexión.
        return response.statusCode() == 200;
    }
}
