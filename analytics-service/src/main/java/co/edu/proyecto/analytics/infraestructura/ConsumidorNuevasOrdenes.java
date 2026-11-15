package co.edu.proyecto.analytics.infraestructura;

import co.edu.proyecto.analytics.aplicacion.AgregadorVentas;
import co.edu.proyecto.common.contratos.OrdenCreada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorNuevasOrdenes {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNuevasOrdenes.class);

    private final AgregadorVentas agregador;

    public ConsumidorNuevasOrdenes(AgregadorVentas agregador) {
        this.agregador = agregador;
    }

    @KafkaListener(topics = "new_orders", groupId = "analytics-group")
    public void consumir(OrdenCreada evento) {
        agregador.registrar(evento.monto());
        log.info("Orden {} | órdenes acumuladas: {} | total acumulado: {} COP",
                evento.orderId(), agregador.cantidadOrdenes(), agregador.totalAcumulado());
    }
}
