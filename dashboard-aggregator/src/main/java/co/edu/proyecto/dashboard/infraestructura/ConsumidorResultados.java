package co.edu.proyecto.dashboard.infraestructura;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.dashboard.estado.EstadoDashboard;
import co.edu.proyecto.dashboard.estado.RepositorioEstados;
import co.edu.proyecto.dashboard.websocket.ManejadorWebSocketOrdenes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Un listener por topic de resultados. */
@Component
public class ConsumidorResultados {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorResultados.class);

    private final RepositorioEstados repositorio;
    private final ManejadorWebSocketOrdenes webSocket;

    public ConsumidorResultados(RepositorioEstados repositorio, ManejadorWebSocketOrdenes webSocket) {
        this.repositorio = repositorio;
        this.webSocket = webSocket;
    }

    @KafkaListener(topics = "billing_events", groupId = "dashboard-group")
    public void pago(ResultadoTarea resultado) {
        emitir("pago", resultado, repositorio.registrarPago(resultado.orderId(), resultado.status()));
    }

    @KafkaListener(topics = "inventory_events", groupId = "dashboard-group")
    public void stock(ResultadoTarea resultado) {
        emitir("stock", resultado, repositorio.registrarStock(resultado.orderId(), resultado.status()));
    }

    @KafkaListener(topics = "notification_events", groupId = "dashboard-group")
    public void email(ResultadoTarea resultado) {
        emitir("correo", resultado, repositorio.registrarEmail(resultado.orderId(), resultado.status()));
    }

    private void emitir(String tipo, ResultadoTarea resultado, EstadoDashboard estado) {
        log.info("[dashboard] {} de {} = {} -> {}", tipo, resultado.orderId(), resultado.status(), estado);
        webSocket.emitir(resultado.orderId(), estado);
    }
}
