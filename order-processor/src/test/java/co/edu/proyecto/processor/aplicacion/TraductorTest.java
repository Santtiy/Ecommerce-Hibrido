package co.edu.proyecto.processor.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraductorTest {

    @Test
    void ordenCreadaSeTraduceAProcesarOrdenConservandoOrderIdItemCantidadYMonto() {
        // Arrange
        TraductorEventoAComando traductor = new TraductorEventoAComando();
        OrdenCreada evento = new OrdenCreada("9f1c2e7a-0000-4000-8000-000000000001", "Ana Perez",
                "SKU-1", 3, new BigDecimal("160650.00"), "COP", Instant.parse("2026-11-14T10:00:00Z"));

        // Act
        ProcesarOrden comando = traductor.traducir(evento);

        // Assert
        assertEquals(evento.orderId(), comando.orderId());
        assertEquals("SKU-1", comando.item());
        assertEquals(3, comando.cantidad());
        assertEquals(new BigDecimal("160650.00"), comando.monto());
        assertNotNull(comando.solicitadoEn());
    }
}
