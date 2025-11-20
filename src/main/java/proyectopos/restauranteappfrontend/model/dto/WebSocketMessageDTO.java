package proyectopos.restauranteappfrontend.model.dto;

public class WebSocketMessageDTO {
    private String type;
    private com.google.gson.JsonElement payload; 
    public String getType() { return type; }
    public com.google.gson.JsonElement getPayload() { return payload; }
}