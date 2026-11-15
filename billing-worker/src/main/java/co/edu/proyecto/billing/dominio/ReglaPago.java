package co.edu.proyecto.billing.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;

import java.math.BigDecimal;
import java.time.Instant;

/** RN-11: un pago por encima de 1.000.000 se rechaza. Clase pura, sin Spring. */
public class ReglaPago {

    public static final BigDecimal LIMITE = new BigDecimal("1000000");
    public static final String EXITO = "PAYMENT_SUCCESS";
    public static final String FALLO = "PAYMENT_FAILED";

    public ResultadoTarea evaluar(ProcesarOrden comando) {
        if (comando.monto().compareTo(LIMITE) > 0) {
            return new ResultadoTarea(comando.orderId(), FALLO,
                    "El monto " + comando.monto().toPlainString() + " supera el límite de 1000000",
                    Instant.now());
        }
        return new ResultadoTarea(comando.orderId(), EXITO, "Pago aprobado", Instant.now());
    }
}
