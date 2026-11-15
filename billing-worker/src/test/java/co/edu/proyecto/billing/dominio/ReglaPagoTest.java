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
        // Arrange
        ProcesarOrden comando = comandoPor("1100000");

        // Act
        ResultadoTarea resultado = regla.evaluar(comando);

        // Assert
        assertEquals("PAYMENT_FAILED", resultado.status());
        assertEquals("orden-1", resultado.orderId());
    }

    @Test
    void unMontoDe900000DaPaymentSuccess() {
        // Arrange
        ProcesarOrden comando = comandoPor("900000");

        // Act
        ResultadoTarea resultado = regla.evaluar(comando);

        // Assert
        assertEquals("PAYMENT_SUCCESS", resultado.status());
    }

    @Test
    void unMontoDeExactamente1000000DaPaymentSuccess() {
        // Arrange: límite exacto, la regla es "mayor que"
        ProcesarOrden comando = comandoPor("1000000.00");

        // Act
        ResultadoTarea resultado = regla.evaluar(comando);

        // Assert
        assertEquals("PAYMENT_SUCCESS", resultado.status());
    }
}
