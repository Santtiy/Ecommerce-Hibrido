package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.orden.dominio.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.IvaColombia;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrdenMapperTest {

    private final OrdenMapper mapper = new OrdenMapper();

    private static Orden ordenDePrueba() {
        Orden orden = new OrdenBuilder()
                .cliente("Ana Pérez")
                .agregarItem(new ItemOrden("SKU-1", "Monitor", 2, Dinero.de("60000", "COP")))
                .agregarItem(new ItemOrden("SKU-2", "Mouse", 1, Dinero.de("30000.50", "COP")))
                .construir();
        orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());
        orden.confirmarPago();
        return orden;
    }

    @Test
    void idaYVueltaConservaIdClienteEstadoTotalYNumeroDeItems() {
        Orden original = ordenDePrueba();

        Orden reconstruida = mapper.aDominio(mapper.aEntidad(original));

        assertEquals(original.id(), reconstruida.id());
        assertEquals(original.cliente(), reconstruida.cliente());
        assertEquals(original.estado(), reconstruida.estado());
        assertEquals(original.total(), reconstruida.total());
        assertEquals(original.items().size(), reconstruida.items().size());
    }

    @Test
    void elMapeoTrasladaMontoYMonedaDeCadaItem() {
        Orden orden = ordenDePrueba();

        OrdenEntity entidad = mapper.aEntidad(orden);

        assertEquals(2, entidad.getItems().size());
        ItemOrdenEntity primero = entidad.getItems().get(0);
        ItemOrdenEntity segundo = entidad.getItems().get(1);
        assertEquals(new BigDecimal("60000.00"), primero.getPrecioMonto());
        assertEquals("COP", primero.getPrecioMoneda());
        assertEquals(new BigDecimal("30000.50"), segundo.getPrecioMonto());
        assertEquals("COP", segundo.getPrecioMoneda());
        assertEquals(entidad, primero.getOrden());
    }
}
