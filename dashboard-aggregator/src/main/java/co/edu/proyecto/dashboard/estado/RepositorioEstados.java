package co.edu.proyecto.dashboard.estado;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Estado actual de cada orden, indexado por orderId.
 *
 * <p>Concurrencia: tres consumidores de Kafka (pago, stock, correo) escriben sobre este mismo mapa
 * a la vez desde hilos distintos. Un HashMap normal se corrompería (entradas perdidas o, durante un
 * redimensionado, estructura interna inconsistente). Además, leer-modificar-escribir en dos pasos
 * perdería actualizaciones: dos hilos leerían el mismo estado y el segundo pisaría al primero.
 * {@link ConcurrentHashMap#compute} hace esa lectura-modificación-escritura de forma ATÓMICA por clave.
 *
 * <p>Idempotencia: cada resultado FIJA un campo a un valor, no lo incrementa. Recibir dos veces el
 * mismo resultado deja el estado igual, lo que protege frente a mensajes duplicados (Kafka y
 * RabbitMQ garantizan entrega "al menos una vez", y los reintentos pueden repetir un resultado).
 */
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

    /** Copia de todos los estados, para enviar la foto completa a un navegador recién conectado. */
    public Map<String, EstadoDashboard> todos() {
        return Map.copyOf(estados);
    }

    private EstadoDashboard actualizar(String orderId, UnaryOperator<EstadoDashboard> cambio) {
        return estados.compute(orderId,
                (id, actual) -> cambio.apply(actual == null ? EstadoDashboard.inicial() : actual));
    }
}
