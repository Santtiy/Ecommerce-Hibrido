package co.edu.proyecto.orden.infraestructura.mensajeria;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.configuracion.ConfiguracionKafka;
import co.edu.proyecto.orden.dominio.puertos.PublicadorEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Adaptador. */
@Component
@Profile("!sin-kafka")
public class KafkaPublicador implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublicador.class);

    private final KafkaTemplate<String, OrdenCreada> kafkaTemplate;

    public KafkaPublicador(KafkaTemplate<String, OrdenCreada> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publicar(OrdenCreada evento) {
        kafkaTemplate.send(ConfiguracionKafka.TOPIC_NUEVAS_ORDENES, evento.orderId(), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("No se pudo publicar OrdenCreada {}", evento.orderId(), error);
                    } else {
                        log.info("OrdenCreada {} publicada en {} (partición {}, offset {})",
                                evento.orderId(),
                                resultado.getRecordMetadata().topic(),
                                resultado.getRecordMetadata().partition(),
                                resultado.getRecordMetadata().offset());
                    }
                });
    }
}
