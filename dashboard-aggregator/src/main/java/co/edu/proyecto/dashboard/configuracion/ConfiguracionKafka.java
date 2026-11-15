package co.edu.proyecto.dashboard.configuracion;

import co.edu.proyecto.common.contratos.ResultadoTarea;
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
 * Consumidor de los tres topics de resultados. Grupo "dashboard-group", distinto de
 * analytics-group y processor-group: así recibe su propia copia de cada evento.
 */
@Configuration
public class ConfiguracionKafka {

    /** ObjectMapper de Spring (java.time), sin cabeceras de tipo; mensajes corruptos se saltan. */
    @Bean
    public ConsumerFactory<String, ResultadoTarea> consumerFactory(KafkaProperties propiedades,
                                                                  SslBundles sslBundles,
                                                                  ObjectMapper objectMapper) {
        return new DefaultKafkaConsumerFactory<>(
                propiedades.buildConsumerProperties(sslBundles),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(
                        new JsonDeserializer<>(ResultadoTarea.class, objectMapper, false)));
    }

    /** Declarada a mano: la autoconfigurada espera un ConsumerFactory&lt;Object, Object&gt;. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ResultadoTarea> kafkaListenerContainerFactory(
            ConsumerFactory<String, ResultadoTarea> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ResultadoTarea> fabrica =
                new ConcurrentKafkaListenerContainerFactory<>();
        fabrica.setConsumerFactory(consumerFactory);
        return fabrica;
    }
}
