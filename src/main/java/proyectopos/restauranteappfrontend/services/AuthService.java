package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import proyectopos.restauranteappfrontend.model.LoginRequest;
import proyectopos.restauranteappfrontend.model.LoginResponse;
import proyectopos.restauranteappfrontend.util.AppConfig; // <-- CAMBIO: Importar AppConfig

public class AuthService {

    // <-- CAMBIO: Eliminar BASE_URL y LOGIN_ENDPOINT estáticos
    private final String loginEndpoint; // <-- CAMBIO: Añadir variable de instancia
    private final HttpClient httpClient;
    private final Gson gson;

    public AuthService() {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        this.gson = new Gson();
        
        // <-- CAMBIO: Construir el endpoint usando AppConfig
        String baseUrl = AppConfig.getInstance().getApiBaseUrl(); 
        if (baseUrl == null || baseUrl.isBlank()) {
            System.err.println("FATAL: 'api.baseUrl' no encontrada en config.properties");
            this.loginEndpoint = null; // O lanzar excepción
        } else {
            this.loginEndpoint = baseUrl + "/api/auth/login";
        }
    }

    public LoginResponse authenticate(String username, String password) throws IOException, InterruptedException {
        // <-- CAMBIO: Validar que el endpoint se haya cargado
        if (this.loginEndpoint == null) {
            throw new IOException("La URL de autenticación no está configurada.");
        }
        
        LoginRequest loginRequest = new LoginRequest(username, password);
        String jsonPayload = gson.toJson(loginRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.loginEndpoint)) // <-- CAMBIO: Usar this.loginEndpoint
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LoginResponse loginResponse = gson.fromJson(response.body(), LoginResponse.class);
            return loginResponse;
        } else {
            System.err.println("Error en login - Status: " + response.statusCode() + ", Body: " + response.body());
            return null;
        }
    }
}