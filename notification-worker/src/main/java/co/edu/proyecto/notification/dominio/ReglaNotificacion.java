package co.edu.proyecto.notification.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;

import java.time.Instant;

/**
 * RN-09: el correo de confirmación siempre se envía (simulado). Clase pura, sin Spring.
 *
 * <p>Limitación conocida y aceptada del diseño: el correo se envía EN PARALELO y NO depende del
 * resultado del pago ni del inventario. El exchange fanout entrega el mismo comando a las tres
 * colas a la vez, así que este worker no sabe si el pago fue rechazado o no hubo stock.
 */
public class ReglaNotificacion {

    public static final String EXITO = "EMAIL_SENT";

    public ResultadoTarea evaluar(ProcesarOrden comando) {
        return new ResultadoTarea(comando.orderId(), EXITO,
                "Correo de confirmación enviado para la orden " + comando.orderId(), Instant.now());
    }
}
