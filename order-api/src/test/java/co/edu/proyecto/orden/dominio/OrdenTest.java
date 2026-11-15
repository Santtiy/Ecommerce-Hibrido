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
        // Arrange
        OrdenBuilder builder = new OrdenBuilder().cliente("Ana Pérez");

        // Act + Assert
        assertThrows(OrdenInvalidaException.class, builder::construir);
    }

    @Test
    void unaOrdenRecienConstruidaQuedaEnEstadoCreada() {
        // Arrange + Act
        Orden orden = ordenConPrecio("50000");

        // Assert
        assertEquals(EstadoOrden.CREADA, orden.estado());
    }

    @Test
    void confirmarPagoCambiaElEstadoAPagada() {
        // Arrange
        Orden orden = ordenConPrecio("50000");

        // Act
        orden.confirmarPago();

        // Assert
        assertEquals(EstadoOrden.PAGADA, orden.estado());
    }

    @Test
    void cancelarCambiaElEstadoACancelada() {
        // Arrange
        Orden orden = ordenConPrecio("50000");

        // Act
        orden.cancelar();

        // Assert
        assertEquals(EstadoOrden.CANCELADA, orden.estado());
    }

    @Test
    void cancelarSobreUnaOrdenPagadaLanzaEstadoInvalidoException() {
        // Arrange
        Orden orden = ordenConPrecio("50000");
        orden.confirmarPago();

        // Act
        EstadoInvalidoException error = assertThrows(EstadoInvalidoException.class, orden::cancelar);

        // Assert
        assertEquals("Una orden pagada no puede cancelarse", error.getMessage());
        assertEquals(EstadoOrden.PAGADA, orden.estado());
    }

    @Test
    void confirmarPagoDosVecesLanzaEstadoInvalidoException() {
        // Arrange
        Orden orden = ordenConPrecio("50000");
        orden.confirmarPago();

        // Act + Assert
        assertThrows(EstadoInvalidoException.class, orden::confirmarPago);
    }

    @Test
    void calcularTotalAplicaPrimeroElDescuentoYDespuesElIva() {
        // Arrange: con 90.000 el orden importa.
        //   Descuento primero: 90.000 no supera 100.000 -> sin descuento -> +19 % = 107.100
        //   IVA primero: 107.100 supera 100.000 -> -10 % = 96.390 (resultado incorrecto)
        Orden orden = ordenConPrecio("90000");

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertEquals(Dinero.de("107100.00", "COP"), total);
        assertEquals(total, orden.total());
    }
}
