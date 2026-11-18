package co.edu.proyecto.notification.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;

import java.time.Instant;

/** RN-09. */
public class ReglaNotificacion {

    public static final String EXITO = "EMAIL_SENT";

    public ResultadoTarea evaluar(ProcesarOrden comando) {
        return new ResultadoTarea(comando.orderId(), EXITO,
                "Correo de confirmación enviado para la orden " + comando.orderId(), Instant.now());
    }
}
