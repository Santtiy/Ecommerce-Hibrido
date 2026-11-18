package co.edu.proyecto.processor.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Traduce un EVENTO en un COMANDO. */
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
