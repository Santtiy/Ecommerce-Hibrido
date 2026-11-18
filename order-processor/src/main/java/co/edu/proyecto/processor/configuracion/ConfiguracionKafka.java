package co.edu.proyecto.processor.configuracion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/** Consumidor de Kafka de order-processor (mismo esquema que analytics-service). */
@Configuration
public class ConfiguracionKafka {

    private static final int HILOS_CONSUMIDOR = 3;

    @Bean
    public ConsumerFactory<String, OrdenCreada> consumerFactory(KafkaProperties propiedades,
                                                               SslBundles sslBundles,
                                                               ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
                propiedades.buildConsumerProperties(sslBundles),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(
                        new JsonDeserializer<>(OrdenCreada.class, objectMapper, false)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrdenCreada> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrdenCreada> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, OrdenCreada> fabrica =
                new ConcurrentKafkaListenerContainerFactory<>();
        fabrica.setConsumerFactory(consumerFactory);
        fabrica.setConcurrency(HILOS_CONSUMIDOR);
        return fabrica;
    }
}
