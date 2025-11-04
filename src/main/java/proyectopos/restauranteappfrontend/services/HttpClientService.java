package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.lang.reflect.Type; // <-- CAMBIO: Importar AppConfig
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
 
import com.google.gson.Gson;

import proyectopos.restauranteappfrontend.util.AppConfig;
import proyectopos.restauranteappfrontend.util.SessionManager; 

public class HttpClientService {

    // <-- CAMBIO: Eliminar la variable BASE_URL estática
    private final String baseUrl; // <-- CAMBIO: Añadir variable de instancia
    private final HttpClient httpClient;
    private final Gson gson;
    private final SessionManager sessionManager;

    public HttpClientService() {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        this.gson = new Gson();
        this.sessionManager = SessionManager.getInstance();
        this.baseUrl = AppConfig.getInstance().getApiBaseUrl(); // <-- CAMBIO: Obtener la URL desde AppConfig
    }

    // --- Método GET Genérico ---
    public <T> T get(String endpoint, Type responseType) throws IOException, InterruptedException, AuthenticationException {
        HttpRequest request = buildAuthenticatedRequest(endpoint)
                .GET()
                .build();
        return sendRequest(request, responseType);
    }

    // --- Método POST Genérico ---
    public <T> T post(String endpoint, Object bodyPayload, Type responseType) throws IOException, InterruptedException, AuthenticationException {
        String jsonPayload = gson.toJson(bodyPayload);
        HttpRequest request = buildAuthenticatedRequest(endpoint)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return sendRequest(request, responseType);
    }

    // --- AÑADIDO: Método PUT Genérico ---
    public <T> T put(String endpoint, Object bodyPayload, Type responseType) throws IOException, InterruptedException, AuthenticationException {
        String jsonPayload = gson.toJson(bodyPayload);
        HttpRequest request = buildAuthenticatedRequest(endpoint)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return sendRequest(request, responseType);
    }

    // --- AÑADIDO: Método DELETE Genérico ---
    public <T> T delete(String endpoint, Type responseType) throws IOException, InterruptedException, AuthenticationException {
        HttpRequest request = buildAuthenticatedRequest(endpoint)
                .DELETE()
                .build();
        return sendRequest(request, responseType);
    }


    // --- Método auxiliar para construir la petición con Token ---
    private HttpRequest.Builder buildAuthenticatedRequest(String endpoint) throws AuthenticationException, IOException { // <-- CAMBIO: Añadir IOException
        String token = sessionManager.getToken();
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("No autenticado. Por favor, inicie sesión.");
        }
        
        // <-- CAMBIO: Validar que la URL base se haya cargado
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            throw new IOException("La URL de la API no está configurada (baseUrl está vacía).");
        }
        
        return HttpRequest.newBuilder()
                .uri(URI.create(this.baseUrl + endpoint)) // <-- CAMBIO: Usar this.baseUrl
                .header("Authorization", "Bearer " + token);
    }

    // ... (El resto de la clase: sendRequest y AuthenticationException no cambian) ...
    private <T> T sendRequest(HttpRequest request, Type responseType) throws IOException, InterruptedException, AuthenticationException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return gson.fromJson(response.body(), responseType);
        } else if (response.statusCode() == 204) {
            return null; 
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            System.err.println("Error de autenticación/autorización - Status: " + response.statusCode());
            sessionManager.clearSession(); 
            throw new AuthenticationException("Sesión inválida o expirada. Por favor, inicie sesión de nuevo.");
        } else {
            System.err.println("Error del servidor - Status: " + response.statusCode() + ", Body: " + response.body());
            throw new IOException("Error del servidor: " + response.statusCode() + " - " + response.body());
        }
    }

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}