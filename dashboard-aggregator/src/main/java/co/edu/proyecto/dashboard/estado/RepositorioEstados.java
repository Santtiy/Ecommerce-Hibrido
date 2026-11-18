package co.edu.proyecto.dashboard.estado;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Estado actual de cada orden, indexado por orderId. */
@Component
public class RepositorioEstados {

    private final ConcurrentHashMap<String, EstadoDashboard> estados = new ConcurrentHashMap<>();

    public EstadoDashboard registrarPago(String orderId, String status) {
        return actualizar(orderId, estado -> estado.conPago(status));
    }

    public EstadoDashboard registrarStock(String orderId, String status) {
        return actualizar(orderId, estado -> estado.conStock(status));
    }

    public EstadoDashboard registrarEmail(String orderId, String status) {
        return actualizar(orderId, estado -> estado.conEmail(status));
    }

    public EstadoDashboard obtener(String orderId) {
        return estados.get(orderId);
    }

    public Map<String, EstadoDashboard> todos() {
        return Map.copyOf(estados);
    }

    private EstadoDashboard actualizar(String orderId, UnaryOperator<EstadoDashboard> cambio) {
        return estados.compute(orderId,
                (id, actual) -> cambio.apply(actual == null ? EstadoDashboard.inicial() : actual));
    }
}
