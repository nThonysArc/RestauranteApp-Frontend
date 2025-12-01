package proyectopos.restauranteappfrontend.services;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List; // IMPRESCINDIBLE
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeType;
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
        
        // --- SOLUCIÓN DEFINITIVA Y SIN ERRORES DE COMPILACIÓN ---
        // En lugar de configurar el converter (que daba error), creamos uno personalizado
        // que acepta JSON sí o sí.
        this.stompClient.setMessageConverter(new StringMessageConverter() {
            @Override
            public List<MimeType> getSupportedMimeTypes() {
                // Definimos manualmente la lista para evitar problemas con Arrays.asList
                List<MimeType> types = new ArrayList<>();
                types.add(new MimeType("application", "json")); // Aceptar JSON
                types.add(new MimeType("text", "plain"));       // Aceptar Texto
                types.add(new MimeType("*", "*"));              // Aceptar Todo
                return types;
            }
        });
        // --------------------------------------------------------

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
                    exception.printStackTrace(); 
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
                // Pedimos String. Nuestro converter personalizado se encargará.
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Ahora es seguro que llegará como String
                if (payload instanceof String) {
                    messageHandler.accept((String) payload);
                } else if (payload instanceof byte[]) {
                    String jsonMessage = new String((byte[]) payload);
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