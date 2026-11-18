package co.edu.proyecto.orden.dominio;

/** Patrón Strategy. */
public interface EstrategiaDescuento {

    Dinero aplicar(Dinero subtotal);
}
