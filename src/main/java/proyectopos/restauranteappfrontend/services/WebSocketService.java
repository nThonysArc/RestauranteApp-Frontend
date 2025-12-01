package proyectopos.restauranteappfrontend.services;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import proyectopos.restauranteappfrontend.util.AppConfig;

public class WebSocketService {

    private static WebSocketService instance;
    private StompSession stompSession;
    private final WebSocketStompClient stompClient;
    private final String wsUrl;
    private final ScheduledExecutorService reconnectScheduler;

    private WebSocketService() {
        WebSocketClient client = new StandardWebSocketClient();
        this.stompClient = new WebSocketStompClient(client);
        
        // SOLUCIÓN DEFINITIVA:
        // Usamos ByteArrayMessageConverter. 
        // Acepta "application/json" sin necesidad de configurar MimeTypes manualmente.
        // Esto elimina la línea que causaba el error de compilación.
        this.stompClient.setMessageConverter(new ByteArrayMessageConverter());

        String apiUrl = AppConfig.getInstance().getApiBaseUrl();
        if (apiUrl != null) {
            this.wsUrl = apiUrl.replace("http", "ws") + "/ws";
        } else {
            this.wsUrl = "ws://localhost:8080/ws";
        }

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
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.err.println("Error de transporte: " + exception.getMessage());
                    scheduleReconnect();
                }
            });
        } catch (Exception e) {
            System.err.println("Fallo al conectar: " + e.getMessage());
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        if (!reconnectScheduler.isShutdown()) {
            reconnectScheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    public void subscribe(String topic, Consumer<String> messageHandler) {
        if (stompSession == null || !stompSession.isConnected()) {
            connect();
            reconnectScheduler.schedule(() -> subscribe(topic, messageHandler), 2, TimeUnit.SECONDS);
            return;
        }

        stompSession.subscribe(topic, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                // Pedimos los datos como arreglo de bytes
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Convertimos los bytes a String manualmente para que el resto de tu app siga igual
                if (payload instanceof byte[]) {
                    String jsonMessage = new String((byte[]) payload, StandardCharsets.UTF_8);
                    messageHandler.accept(jsonMessage);
                }
            }
        });
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        reconnectScheduler.shutdownNow();
    }
}