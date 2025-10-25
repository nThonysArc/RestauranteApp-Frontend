package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import proyectopos.restauranteappfrontend.model.LoginRequest;
import proyectopos.restauranteappfrontend.model.LoginResponse;

public class AuthService {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/api/auth/login";

    private final HttpClient httpClient;
    private final Gson gson;

    public AuthService() {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        this.gson = new Gson();
    }

    /**
     * Intenta autenticar al usuario contra el backend.
     * @param username Nombre de usuario.
     * @param password Contraseña.
     * @return El objeto LoginResponse completo (token + datos de usuario) o null si falla. // <-- MODIFICADO
     * @throws IOException Si hay un error de red o I/O.
     * @throws InterruptedException Si la operación es interrumpida.
     */
    // <-- MODIFICADO: El tipo de retorno ahora es LoginResponse
    public LoginResponse authenticate(String username, String password) throws IOException, InterruptedException {
        LoginRequest loginRequest = new LoginRequest(username, password);
        String jsonPayload = gson.toJson(loginRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Deserializar la respuesta JSON completa
            LoginResponse loginResponse = gson.fromJson(response.body(), LoginResponse.class);
            // <-- MODIFICADO: Devolvemos el objeto completo
            return loginResponse;
        } else {
            System.err.println("Error en login - Status: " + response.statusCode() + ", Body: " + response.body());
            return null;
        }
    }
}