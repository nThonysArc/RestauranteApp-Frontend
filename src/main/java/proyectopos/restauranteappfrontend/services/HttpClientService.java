package proyectopos.restauranteappfrontend.services;

import com.google.gson.Gson;
import proyectopos.restauranteappfrontend.util.SessionManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.lang.reflect.Type; // Para listas genéricas

public class HttpClientService {

    private static final String BASE_URL = "http://localhost:8080"; // Mantenlo consistente
    private final HttpClient httpClient;
    private final Gson gson;
    private final SessionManager sessionManager;

    public HttpClientService() {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        this.gson = new Gson();
        this.sessionManager = SessionManager.getInstance();
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

    // --- (Añadir métodos PUT, DELETE genéricos si los necesitas) ---

    // --- Método auxiliar para construir la petición con Token ---
    private HttpRequest.Builder buildAuthenticatedRequest(String endpoint) throws AuthenticationException {
        String token = sessionManager.getToken();
        if (token == null || token.isBlank()) {
            // Lanza una excepción si no hay token (el usuario debería ser redirigido al login)
            throw new AuthenticationException("No autenticado. Por favor, inicie sesión.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Authorization", "Bearer " + token);
    }

    // --- Método auxiliar para enviar la petición y procesar respuesta ---
    private <T> T sendRequest(HttpRequest request, Type responseType) throws IOException, InterruptedException, AuthenticationException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Deserializa el cuerpo JSON al tipo esperado (puede ser un objeto o una lista)
            return gson.fromJson(response.body(), responseType);
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            // Error de autenticación o autorización
            System.err.println("Error de autenticación/autorización - Status: " + response.statusCode());
            sessionManager.clearSession(); // Limpiar sesión inválida
            throw new AuthenticationException("Sesión inválida o expirada. Por favor, inicie sesión de nuevo.");
        } else {
            // Otros errores del servidor
            System.err.println("Error del servidor - Status: " + response.statusCode() + ", Body: " + response.body());
            // Lanza una excepción genérica de I/O o crea una personalizada
            throw new IOException("Error del servidor: " + response.statusCode());
        }
    }

    // --- Clase de Excepción Personalizada para Autenticación ---
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}