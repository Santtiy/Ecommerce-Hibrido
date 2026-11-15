package co.edu.proyecto.orden.configuracion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Productor de Kafka de order-api.
 *
 * <p>ADVERTENCIA sobre grupos de consumidores: analytics-service consume este topic con el
 * grupo "analytics-group" y order_processor (etapa 4) usará "processor-group". Deben ser
 * DISTINTOS: si coincidieran, Kafka los trataría como dos instancias del mismo consumidor y
 * entregaría cada evento a uno solo de los dos, procesando la mitad de las órdenes sin lanzar
 * ninguna excepción.
 */
@Configuration
@Profile("!sin-kafka")
public class ConfiguracionKafka {

    public static final String TOPIC_NUEVAS_ORDENES = "new_orders";

    /** KafkaAdmin (autoconfigurado por Spring Boot) crea el topic al arrancar si no existe. */
    @Bean
    public NewTopic nuevasOrdenes() {
        return TopicBuilder.name(TOPIC_NUEVAS_ORDENES).partitions(3).replicas(1).build();
    }

    /**
     * Fábrica explícita para inyectar el ObjectMapper de Spring, que trae el módulo de
     * java.time. El JsonSerializer por defecto crea el suyo propio y no sabe serializar Instant.
     */
    @Bean
    public ProducerFactory<String, OrdenCreada> producerFactory(KafkaProperties propiedades,
                                                               SslBundles sslBundles,
                                                               ObjectMapper objectMapper) {
        return new DefaultKafkaProducerFactory<>(
                propiedades.buildProducerProperties(sslBundles),
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, OrdenCreada> kafkaTemplate(ProducerFactory<String, OrdenCreada> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
