package proyectopos.restauranteappfrontend.services;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import proyectopos.restauranteappfrontend.util.AppConfig;
import proyectopos.restauranteappfrontend.util.SessionManager;

public class MediaService {

    private final String baseUrl;

    public MediaService() {
        this.baseUrl = AppConfig.getInstance().getApiBaseUrl();
    }

    /**
     * Sube una imagen al servidor y devuelve la URL pública.
     * @param file El archivo de imagen seleccionado del disco local.
     * @return String La URL remota de la imagen (ej. "http://localhost:8080/images/123_foto.jpg").
     * @throws Exception Si ocurre un error de red o del servidor.
     */
    public String uploadImage(File file) throws Exception {
        String uploadEndpoint = baseUrl + "/api/media/upload";
        String token = SessionManager.getInstance().getToken();

        if (token == null || token.isBlank()) {
            throw new RuntimeException("No hay sesión activa. Por favor inicie sesión.");
        }

        // Generar un límite aleatorio para el multipart
        String boundary = new BigInteger(256, new Random()).toString();

        // Preparar los datos del formulario
        Map<String, Object> data = new HashMap<>();
        data.put("file", file.toPath());

        // Construir el cuerpo de la petición
        HttpRequest.BodyPublisher body = ofMimeMultipartData(data, boundary);

        // Crear la petición HTTP POST
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadEndpoint))
                .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                .header("Authorization", "Bearer " + token)
                .POST(body)
                .build();

        // Enviar la petición usando un cliente HTTP nuevo (para no interferir con el genérico)
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Validar respuesta
        if (response.statusCode() == 200) {
            // El backend devuelve la URL como texto plano en el cuerpo
            return response.body();
        } else {
            throw new IOException("Error al subir imagen (" + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Método auxiliar para construir el cuerpo multipart (necesario en Java 11+ estándar).
     */
    private HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, Object> data, String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            byteArrays.add(separator);

            if (entry.getValue() instanceof Path) {
                var path = (Path) entry.getValue();
                String mimeType = Files.probeContentType(path);
                if (mimeType == null) mimeType = "application/octet-stream";
                
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
                        + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}