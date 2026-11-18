package co.edu.proyecto.processor.infraestructura;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.processor.configuracion.ConfiguracionRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorTareas {

    private static final String ROUTING_KEY_IGNORADA = "";

    private final RabbitTemplate rabbitTemplate;

    public PublicadorTareas(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(ProcesarOrden comando) {
        rabbitTemplate.convertAndSend(ConfiguracionRabbit.EXCHANGE_TAREAS, ROUTING_KEY_IGNORADA, comando);
    }
}
