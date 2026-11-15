package co.edu.proyecto.analytics.configuracion;

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

/**
 * Consumidor de Kafka de analytics-service.
 *
 * <p>ADVERTENCIA sobre grupos de consumidores: este servicio usa "analytics-group" y
 * order_processor (etapa 4) usará "processor-group". Deben ser DISTINTOS: si coincidieran,
 * Kafka los trataría como dos instancias del mismo consumidor y entregaría cada evento a uno
 * solo de los dos, procesando la mitad de las órdenes sin lanzar ninguna excepción.
 */
@Configuration
public class ConfiguracionKafka {

    /** Un hilo por partición del topic new_orders (3). */
    private static final int HILOS_CONSUMIDOR = 3;

    /**
     * Fábrica explícita para inyectar el ObjectMapper de Spring (con soporte de java.time).
     * El tercer parámetro en false ignora las cabeceras de tipo: siempre se deserializa a OrdenCreada.
     * ErrorHandlingDeserializer evita que un mensaje corrupto bloquee el consumo en un bucle infinito:
     * se registra el error y se salta ese mensaje.
     */
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

    /**
     * Se declara explícitamente porque la fábrica autoconfigurada de Spring Boot busca un
     * ConsumerFactory&lt;Object, Object&gt; y no usaría el tipado de arriba.
     */
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
