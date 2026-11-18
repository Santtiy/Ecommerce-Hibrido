package co.edu.proyecto.processor.infraestructura;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.processor.aplicacion.TraductorEventoAComando;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Puente Kafka → RabbitMQ. */
@Component
public class ConsumidorNuevasOrdenes {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNuevasOrdenes.class);

    private final TraductorEventoAComando traductor;
    private final PublicadorTareas publicador;

    public ConsumidorNuevasOrdenes(TraductorEventoAComando traductor, PublicadorTareas publicador) {
        this.traductor = traductor;
        this.publicador = publicador;
    }

    @KafkaListener(topics = "new_orders", groupId = "processor-group")
    public void consumir(OrdenCreada evento) {
        ProcesarOrden comando = traductor.traducir(evento);
        publicador.publicar(comando);
        log.info("Orden {} recibida de Kafka y despachada a RabbitMQ (order_tasks)", evento.orderId());
    }
}
