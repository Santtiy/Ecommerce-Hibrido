package co.edu.proyecto.dashboard.configuracion;

import co.edu.proyecto.dashboard.websocket.ManejadorWebSocketOrdenes;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** WebSocket plano (sin STOMP ni SockJS) en /ws/ordenes. */
@Configuration
@EnableWebSocket
public class ConfiguracionWebSocket implements WebSocketConfigurer {

    private final ManejadorWebSocketOrdenes manejador;

    public ConfiguracionWebSocket(ManejadorWebSocketOrdenes manejador) {
        this.manejador = manejador;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Dashboard de solo lectura en entorno local: se acepta cualquier origen
        // (localhost, 127.0.0.1 o la IP de la máquina). Restringirlo en producción.
        registry.addHandler(manejador, "/ws/ordenes").setAllowedOrigins("*");
    }
}
