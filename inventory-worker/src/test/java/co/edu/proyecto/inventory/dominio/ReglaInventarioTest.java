package co.edu.proyecto.inventory.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglaInventarioTest {

    private final ReglaInventario regla = new ReglaInventario();

    private static ProcesarOrden comandoPor(int cantidad) {
        return new ProcesarOrden("orden-1", "SKU-1", cantidad, new BigDecimal("50000"), Instant.now());
    }

    @Test
    void onceUnidadesDanStockOutOfStock() {
        ProcesarOrden comando = comandoPor(11);

        ResultadoTarea resultado = regla.evaluar(comando);

        assertEquals("STOCK_OUT_OF_STOCK", resultado.status());
        assertEquals("orden-1", resultado.orderId());
    }

    @Test
    void diezUnidadesExactasDanStockReserved() {
        ProcesarOrden comando = comandoPor(10);

        ResultadoTarea resultado = regla.evaluar(comando);

        assertEquals("STOCK_RESERVED", resultado.status());
    }
}
