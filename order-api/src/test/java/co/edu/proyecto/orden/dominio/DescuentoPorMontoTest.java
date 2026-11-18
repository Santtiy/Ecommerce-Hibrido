package co.edu.proyecto.orden.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescuentoPorMontoTest {

    private final EstrategiaDescuento descuento = new DescuentoPorMonto();

    @Test
    void descuentoPorMontoAplicaElDiezPorCientoCuandoElSubtotalSupera100000() {
        Dinero subtotal = Dinero.de("200000", "COP");

        Dinero resultado = descuento.aplicar(subtotal);

        assertEquals(Dinero.de("180000.00", "COP"), resultado);
    }

    @Test
    void descuentoPorMontoNoAplicaDescuentoConUnSubtotalDeExactamente100000() {
        Dinero subtotal = Dinero.de("100000", "COP");

        Dinero resultado = descuento.aplicar(subtotal);

        assertEquals(Dinero.de("100000.00", "COP"), resultado);
    }
}
