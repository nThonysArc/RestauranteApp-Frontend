package proyectopos.restauranteappfrontend.services;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import proyectopos.restauranteappfrontend.util.AppConfig;

import java.lang.reflect.Type;
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
        this.stompClient.setMessageConverter(new StringMessageConverter());
        
        // Construye la URL de WebSocket (ws://) desde la URL de la API (http://)
        String apiUrl = AppConfig.getInstance().getApiBaseUrl();
        this.wsUrl = apiUrl.replace("http", "ws") + "/ws";

        // Programador para reintentar la conexión si falla
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
                    scheduleReconnect();
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

    // Método para que los controladores se suscriban
    public void subscribe(String topic, Consumer<String> messageHandler) {
        if (stompSession == null || !stompSession.isConnected()) {
            System.err.println("No se puede suscribir, la sesión no está activa. Intentando reconectar...");
            connect(); // Intenta reconectar
            // Reintenta la suscripción después de conectar
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
                // Llama al "manejador" (el código en el controlador)
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
        reconnectScheduler.shutdownNow(); // Detiene los reintentos
    }
}