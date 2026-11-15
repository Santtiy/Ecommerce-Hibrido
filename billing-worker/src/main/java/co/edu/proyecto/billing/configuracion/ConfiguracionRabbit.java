package co.edu.proyecto.billing.configuracion;

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

/**
 * Topología de RabbitMQ que necesita este worker. Cada worker declara por su cuenta el exchange
 * order_tasks, su propia cola y su binding (y también el DLX y failed_tasks): declarar algo que ya
 * existe con los mismos parámetros es idempotente, así que el orden de arranque no importa.
 */
@Configuration
public class ConfiguracionRabbit {

    public static final String EXCHANGE_TAREAS = "order_tasks";
    public static final String EXCHANGE_FALLOS = "order_tasks_dlx";
    public static final String COLA = "billing_queue";
    public static final String COLA_FALLOS = "failed_tasks";

    @Bean
    public FanoutExchange exchangeTareas() {
        return new FanoutExchange(EXCHANGE_TAREAS, true, false);
    }

    @Bean
    public FanoutExchange exchangeFallos() {
        return new FanoutExchange(EXCHANGE_FALLOS, true, false);
    }

    /** Un mensaje rechazado sin reencolar (tras agotar los reintentos) va a order_tasks_dlx. */
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

    /** Con el ObjectMapper de Spring, que sabe deserializar el Instant de ProcesarOrden. */
    @Bean
    public Jackson2JsonMessageConverter convertidor(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * El configurer aplica spring.rabbitmq.listener.simple.* de application.yml
     * (prefetch, concurrencia, reintentos); después se fija el convertidor JSON.
     */
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
