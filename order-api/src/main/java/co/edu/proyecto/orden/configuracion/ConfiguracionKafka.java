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

/** Productor de Kafka de order-api. */
@Configuration
@Profile("!sin-kafka")
public class ConfiguracionKafka {

    public static final String TOPIC_NUEVAS_ORDENES = "new_orders";

    @Bean
    public NewTopic nuevasOrdenes() {
        return TopicBuilder.name(TOPIC_NUEVAS_ORDENES).partitions(3).replicas(1).build();
    }

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
