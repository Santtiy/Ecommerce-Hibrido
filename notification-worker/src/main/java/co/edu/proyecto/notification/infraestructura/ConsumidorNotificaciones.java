package co.edu.proyecto.notification.infraestructura;

import co.edu.proyecto.notification.configuracion.ConfiguracionKafka;
import co.edu.proyecto.notification.configuracion.ConfiguracionRabbit;
import co.edu.proyecto.notification.dominio.ReglaNotificacion;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ConsumidorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificaciones.class);

    private final ReglaNotificacion regla = new ReglaNotificacion();
    private final KafkaTemplate<String, ResultadoTarea> kafkaTemplate;

    public ConsumidorNotificaciones(KafkaTemplate<String, ResultadoTarea> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @RabbitListener(queues = ConfiguracionRabbit.COLA)
    public void consumir(ProcesarOrden comando) {
        long inicio = System.nanoTime();
        simularTrabajo();

        ResultadoTarea resultado = regla.evaluar(comando);
        publicar(resultado);

        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        log.info("[notification] orden {} -> {} en {} ms (hilo {})",
                comando.orderId(), resultado.status(), ms, Thread.currentThread().getName());
    }

    private void simularTrabajo() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(200, 501));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Trabajo interrumpido", e);
        }
    }

    private void publicar(ResultadoTarea resultado) {
        try {
            kafkaTemplate.send(ConfiguracionKafka.TOPIC, resultado.orderId(), resultado).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido al publicar " + resultado.orderId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("No se pudo publicar el resultado de " + resultado.orderId(), e);
        }
    }
}
