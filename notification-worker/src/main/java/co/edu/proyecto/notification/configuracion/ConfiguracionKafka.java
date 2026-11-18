package co.edu.proyecto.notification.configuracion;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/** Publica los resultados de notificación en Kafka para que el dashboard los consuma. */
@Configuration
public class ConfiguracionKafka {

    public static final String TOPIC = "notification_events";

    @Bean
    public NewTopic topicResultados() {
        return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, ResultadoTarea> producerFactory(KafkaProperties propiedades,
                                                                  SslBundles sslBundles,
                                                                  ObjectMapper objectMapper) {
        return new DefaultKafkaProducerFactory<>(
                propiedades.buildProducerProperties(sslBundles),
                new StringSerializer(),
                new JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, ResultadoTarea> kafkaTemplate(ProducerFactory<String, ResultadoTarea> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
