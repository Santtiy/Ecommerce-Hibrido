package co.edu.proyecto.dashboard.websocket;

import co.edu.proyecto.dashboard.estado.EstadoDashboard;
import co.edu.proyecto.dashboard.estado.RepositorioEstados;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** WebSocket plano. */
@Component
public class ManejadorWebSocketOrdenes extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorWebSocketOrdenes.class);
    private static final int LIMITE_ENVIO_MS = 5_000;
    private static final int LIMITE_BUFFER_BYTES = 512 * 1024;

    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final RepositorioEstados repositorio;
    private final ObjectMapper objectMapper;

    public ManejadorWebSocketOrdenes(RepositorioEstados repositorio, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(session, LIMITE_ENVIO_MS, LIMITE_BUFFER_BYTES);
        sesiones.put(session.getId(), segura);
        Map<String, EstadoDashboard> actuales = repositorio.todos();
        if (!actuales.isEmpty()) {
            segura.sendMessage(new TextMessage(objectMapper.writeValueAsString(actuales)));
        }
        log.info("Navegador conectado ({} abiertos)", sesiones.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session.getId());
        log.info("Navegador desconectado ({} abiertos)", sesiones.size());
    }

    public void emitir(String orderId, EstadoDashboard estado) {
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(orderId, estado));
        } catch (JsonProcessingException e) {
            log.error("No se pudo serializar el estado de {}", orderId, e);
            return;
        }
        TextMessage mensaje = new TextMessage(json);
        sesiones.values().forEach(sesion -> enviar(sesion, mensaje));
    }

    private void enviar(WebSocketSession sesion, TextMessage mensaje) {
        if (!sesion.isOpen()) {
            sesiones.remove(sesion.getId());
            return;
        }
        try {
            sesion.sendMessage(mensaje);
        } catch (IOException | RuntimeException e) {
            log.warn("Falló el envío a la sesión {}; se descarta", sesion.getId(), e);
            sesiones.remove(sesion.getId());
        }
    }
}
