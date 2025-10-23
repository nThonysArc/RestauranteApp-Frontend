package proyectopos.restauranteappfrontend.services;

import com.google.gson.Gson;
import proyectopos.restauranteappfrontend.model.LoginRequest;
import proyectopos.restauranteappfrontend.model.LoginResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
     * @return El token JWT si la autenticación es exitosa (status 200), null en caso contrario.
     * @throws IOException Si hay un error de red o I/O.
     * @throws InterruptedException Si la operación es interrumpida.
     */
    public String authenticate(String username, String password) throws IOException, InterruptedException {
        LoginRequest loginRequest = new LoginRequest(username, password);
        String jsonPayload = gson.toJson(loginRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Deserializar la respuesta JSON para obtener el token
            LoginResponse loginResponse = gson.fromJson(response.body(), LoginResponse.class);
            return loginResponse.getToken();
        } else {
            // Podrías analizar otros códigos de error (401, 403, 500) si quieres
            // y lanzar excepciones específicas. Por ahora, devolvemos null.
            System.err.println("Error en login - Status: " + response.statusCode() + ", Body: " + response.body());
            return null;
        }
    }
}