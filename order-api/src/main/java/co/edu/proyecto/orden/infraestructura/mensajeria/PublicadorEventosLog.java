package co.edu.proyecto.orden.infraestructura.mensajeria;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.dominio.puertos.PublicadorEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Respaldo para demostrar la etapa 2 sin Kafka. */
@Component
@Profile("sin-kafka")
public class PublicadorEventosLog implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventosLog.class);

    @Override
    public void publicar(OrdenCreada evento) {
        log.info("[sin-kafka] Evento OrdenCreada: {}", evento);
    }
}
