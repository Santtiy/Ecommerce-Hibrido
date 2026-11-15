package co.edu.proyecto.processor.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lado productor de RabbitMQ. Solo declara el exchange order_tasks: las colas las declara cada
 * worker. Declarar un exchange que ya existe con los mismos parámetros es idempotente.
 */
@Configuration
public class ConfiguracionRabbit {

    public static final String EXCHANGE_TAREAS = "order_tasks";

    @Bean
    public FanoutExchange exchangeTareas() {
        return new FanoutExchange(EXCHANGE_TAREAS, true, false);
    }

    /** Con el ObjectMapper de Spring, que sabe serializar el Instant de ProcesarOrden. */
    @Bean
    public Jackson2JsonMessageConverter convertidor(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    /** RabbitTemplate con la configuración de application.yml y el convertidor JSON. */
    @Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter convertidor) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connectionFactory);
        template.setMessageConverter(convertidor);
        return template;
    }
}
