package co.edu.proyecto.orden.dominio;

import java.math.BigDecimal;

/** RN-14: si el subtotal supera 100.000 se descuenta el 10 %. Exactamente 100.000 no aplica. */
public class DescuentoPorMonto implements EstrategiaDescuento {

    private static final BigDecimal UMBRAL = new BigDecimal("100000");
    private static final int PORCENTAJE = 10;

    @Override
    public Dinero aplicar(Dinero subtotal) {
        Dinero umbral = new Dinero(UMBRAL, subtotal.moneda());
        if (subtotal.esMayorQue(umbral)) {
            return subtotal.restar(subtotal.porcentaje(PORCENTAJE));
        }
        return subtotal;
    }
}
