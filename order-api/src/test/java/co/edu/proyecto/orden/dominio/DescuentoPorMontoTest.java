package co.edu.proyecto.orden.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescuentoPorMontoTest {

    private final EstrategiaDescuento descuento = new DescuentoPorMonto();

    @Test
    void descuentoPorMontoAplicaElDiezPorCientoCuandoElSubtotalSupera100000() {
        // Arrange
        Dinero subtotal = Dinero.de("200000", "COP");

        // Act
        Dinero resultado = descuento.aplicar(subtotal);

        // Assert
        assertEquals(Dinero.de("180000.00", "COP"), resultado);
    }

    @Test
    void descuentoPorMontoNoAplicaDescuentoConUnSubtotalDeExactamente100000() {
        // Arrange
        Dinero subtotal = Dinero.de("100000", "COP");

        // Act
        Dinero resultado = descuento.aplicar(subtotal);

        // Assert
        assertEquals(Dinero.de("100000.00", "COP"), resultado);
    }
}
