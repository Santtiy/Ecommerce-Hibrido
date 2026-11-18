package co.edu.proyecto.analytics.so;

import co.edu.proyecto.common.contratos.OrdenCreada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Registra cada orden recibida en un archivo y mide el coste de E/S y el uso de memoria. */
@Component
public class RegistroOrdenesListener {

    private static final Logger log = LoggerFactory.getLogger(RegistroOrdenesListener.class);

    private final GestorArchivos gestorArchivos;
    private final MonitorRecursos monitor;

    public RegistroOrdenesListener(GestorArchivos gestorArchivos, MonitorRecursos monitor) {
        this.gestorArchivos = gestorArchivos;
        this.monitor = monitor;
    }

    public void alRecibirOrden(OrdenCreada evento) {
        try {
            String linea = String.join(";", evento.orderId(), evento.monto().toPlainString(), Instant.now().toString());
            ResultadoES resultado = gestorArchivos.escribir(linea);
            log.info("[SO] {} | archivo={} bytes | {}", resultado, gestorArchivos.tamanoActual(), monitor.reportar());
        } catch (RuntimeException e) {
            log.error("[SO] No se pudo registrar la orden {} en el archivo", evento.orderId(), e);
        }
    }
}
