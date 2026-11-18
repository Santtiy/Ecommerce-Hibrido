package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdenTest {

    private static Orden ordenConPrecio(String precioUnitario) {
        return new OrdenBuilder()
                .cliente("Ana Pérez")
                .agregarItem(new ItemOrden("SKU-1", "Monitor", 1, Dinero.de(precioUnitario, "COP")))
                .construir();
    }

    @Test
    void ordenBuilderRechazaConstruirUnaOrdenSinItems() {
        OrdenBuilder builder = new OrdenBuilder().cliente("Ana Pérez");

        assertThrows(OrdenInvalidaException.class, builder::construir);
    }

    @Test
    void unaOrdenRecienConstruidaQuedaEnEstadoCreada() {
        Orden orden = ordenConPrecio("50000");

        assertEquals(EstadoOrden.CREADA, orden.estado());
    }

    @Test
    void confirmarPagoCambiaElEstadoAPagada() {
        Orden orden = ordenConPrecio("50000");

        orden.confirmarPago();

        assertEquals(EstadoOrden.PAGADA, orden.estado());
    }

    @Test
    void cancelarCambiaElEstadoACancelada() {
        Orden orden = ordenConPrecio("50000");

        orden.cancelar();

        assertEquals(EstadoOrden.CANCELADA, orden.estado());
    }

    @Test
    void cancelarSobreUnaOrdenPagadaLanzaEstadoInvalidoException() {
        Orden orden = ordenConPrecio("50000");
        orden.confirmarPago();

        EstadoInvalidoException error = assertThrows(EstadoInvalidoException.class, orden::cancelar);

        assertEquals("Una orden pagada no puede cancelarse", error.getMessage());
        assertEquals(EstadoOrden.PAGADA, orden.estado());
    }

    @Test
    void confirmarPagoDosVecesLanzaEstadoInvalidoException() {
        Orden orden = ordenConPrecio("50000");
        orden.confirmarPago();

        assertThrows(EstadoInvalidoException.class, orden::confirmarPago);
    }

    @Test
    void calcularTotalAplicaPrimeroElDescuentoYDespuesElIva() {
        Orden orden = ordenConPrecio("90000");

        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        assertEquals(Dinero.de("107100.00", "COP"), total);
        assertEquals(total, orden.total());
    }
}
