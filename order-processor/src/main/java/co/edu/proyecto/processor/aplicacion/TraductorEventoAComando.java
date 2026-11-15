package co.edu.proyecto.processor.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Traduce un EVENTO en un COMANDO.
 *
 * <p>Un evento ({@link OrdenCreada}) informa algo que YA ocurrió; puede tener cero o muchos
 * interesados y quien lo emite no espera nada de ellos. Un comando ({@link ProcesarOrden}) pide
 * EJECUTAR una acción concreta y alguien tiene la responsabilidad de hacerla.
 * Por eso los eventos viajan por Kafka (log persistente que cada grupo lee a su ritmo y puede
 * releer) y los comandos por RabbitMQ (colas de trabajo con confirmación, reintentos y DLX).
 */
@Component
public class TraductorEventoAComando {

    public ProcesarOrden traducir(OrdenCreada evento) {
        return new ProcesarOrden(
                evento.orderId(),
                evento.item(),
                evento.cantidad(),
                evento.monto(),
                Instant.now());
    }
}
