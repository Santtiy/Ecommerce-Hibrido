package co.edu.proyecto.billing.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglaPagoTest {

    private final ReglaPago regla = new ReglaPago();

    private static ProcesarOrden comandoPor(String monto) {
        return new ProcesarOrden("orden-1", "SKU-1", 1, new BigDecimal(monto), Instant.now());
    }

    @Test
    void unMontoDe1100000DaPaymentFailed() {
        ProcesarOrden comando = comandoPor("1100000");

        ResultadoTarea resultado = regla.evaluar(comando);

        assertEquals("PAYMENT_FAILED", resultado.status());
        assertEquals("orden-1", resultado.orderId());
    }

    @Test
    void unMontoDe900000DaPaymentSuccess() {
        ProcesarOrden comando = comandoPor("900000");

        ResultadoTarea resultado = regla.evaluar(comando);

        assertEquals("PAYMENT_SUCCESS", resultado.status());
    }

    @Test
    void unMontoDeExactamente1000000DaPaymentSuccess() {
        ProcesarOrden comando = comandoPor("1000000.00");

        ResultadoTarea resultado = regla.evaluar(comando);

        assertEquals("PAYMENT_SUCCESS", resultado.status());
    }
}
