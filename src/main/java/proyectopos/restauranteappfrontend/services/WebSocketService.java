package proyectopos.restauranteappfrontend.services;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeType; // <--- Importar esto
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import proyectopos.restauranteappfrontend.util.AppConfig;

import java.lang.reflect.Type;
import java.util.Collections; // Usar Collections.singletonList es más limpio
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WebSocketService {

    private static WebSocketService instance;
    private StompSession stompSession;
    private final WebSocketStompClient stompClient;
    private final String wsUrl;
    private final ScheduledExecutorService reconnectScheduler;

    private WebSocketService() {
        WebSocketClient client = new StandardWebSocketClient();
        this.stompClient = new WebSocketStompClient(client);

        // --- SOLUCIÓN CORREGIDA ---
        StringMessageConverter converter = new StringMessageConverter();
        
        // Usamos una lista con un MimeType comodín explícito (*/*)
        // Esto evita el error con MimeTypeUtils.ALL si la librería no lo resuelve bien
        converter.setSupportedMimeTypes(Collections.singletonList(new MimeType("*", "*")));
        
        this.stompClient.setMessageConverter(converter);
        // ---------------------------

        String apiUrl = AppConfig.getInstance().getApiBaseUrl();
        this.wsUrl = apiUrl.replace("http", "ws") + "/ws";

        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized WebSocketService getInstance() {
        if (instance == null) {
            instance = new WebSocketService();
        }
        return instance;
    }

    public void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            System.out.println("WebSocket ya está conectado.");
            return;
        }

        try {
            System.out.println("Conectando a WebSocket en: " + wsUrl);
            stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    stompSession = session;
                    System.out.println("¡Conectado a WebSocket! Sesión: " + session.getSessionId());
                }

                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("Error en WebSocket: " + exception.getMessage());
                    // No reconectamos inmediatamente aquí para evitar bucles
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.err.println("Error de transporte en WebSocket: " + exception.getMessage());
                    scheduleReconnect();
                }
            });
        } catch (Exception e) {
            System.err.println("Fallo al iniciar conexión WebSocket: " + e.getMessage());
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        if (!reconnectScheduler.isShutdown()) {
            reconnectScheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
            System.out.println("Reintentando conexión WebSocket en 5 segundos...");
        }
    }

    public void subscribe(String topic, Consumer<String> messageHandler) {
        if (stompSession == null || !stompSession.isConnected()) {
            System.err.println("No se puede suscribir, la sesión no está activa. Intentando reconectar...");
            connect();
            reconnectScheduler.schedule(() -> subscribe(topic, messageHandler), 6, TimeUnit.SECONDS);
            return;
        }

        stompSession.subscribe(topic, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageHandler.accept((String) payload);
            }
        });
        System.out.println("Suscrito a: " + topic);
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            System.out.println("WebSocket desconectado.");
        }
        reconnectScheduler.shutdownNow();
    }
}