package proyectopos.restauranteappfrontend.services;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import proyectopos.restauranteappfrontend.model.dto.PedidoWebDTO;

public class PedidoWebService {

    private final HttpClientService httpClient;

    public PedidoWebService() {
        this.httpClient = new HttpClientService();
    }

    // Obtiene los pedidos web activos (PENDIENTE, EN_COCINA)
    public List<PedidoWebDTO> getPedidosWebActivos() throws IOException, InterruptedException, HttpClientService.AuthenticationException {
        Type listType = new TypeToken<List<PedidoWebDTO>>(){}.getType();
        // Este endpoint ya existe en tu backend (PedidoWebController.java)
        return httpClient.get("/api/web/pedidos/activos", listType);
    }
}