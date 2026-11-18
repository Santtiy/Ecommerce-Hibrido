package co.edu.proyecto.inventory.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Topología de RabbitMQ que necesita este worker. */
@Configuration
public class ConfiguracionRabbit {

    public static final String EXCHANGE_TAREAS = "order_tasks";
    public static final String EXCHANGE_FALLOS = "order_tasks_dlx";
    public static final String COLA = "inventory_queue";
    public static final String COLA_FALLOS = "failed_tasks";

    @Bean
    public FanoutExchange exchangeTareas() {
        return new FanoutExchange(EXCHANGE_TAREAS, true, false);
    }

    @Bean
    public FanoutExchange exchangeFallos() {
        return new FanoutExchange(EXCHANGE_FALLOS, true, false);
    }

    @Bean
    public Queue cola() {
        return QueueBuilder.durable(COLA).deadLetterExchange(EXCHANGE_FALLOS).build();
    }

    @Bean
    public Queue colaFallos() {
        return QueueBuilder.durable(COLA_FALLOS).build();
    }

    @Bean
    public Binding enlaceCola() {
        return BindingBuilder.bind(cola()).to(exchangeTareas());
    }

    @Bean
    public Binding enlaceFallos() {
        return BindingBuilder.bind(colaFallos()).to(exchangeFallos());
    }

    @Bean
    public Jackson2JsonMessageConverter convertidor(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter convertidor) {
        SimpleRabbitListenerContainerFactory fabrica = new SimpleRabbitListenerContainerFactory();
        configurer.configure(fabrica, connectionFactory);
        fabrica.setMessageConverter(convertidor);
        return fabrica;
    }
}
