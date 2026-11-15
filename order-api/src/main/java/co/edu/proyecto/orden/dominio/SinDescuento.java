package co.edu.proyecto.orden.dominio;

public class SinDescuento implements EstrategiaDescuento {

    @Override
    public Dinero aplicar(Dinero subtotal) {
        return subtotal;
    }
}
