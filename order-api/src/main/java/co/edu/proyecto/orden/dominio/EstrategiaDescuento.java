package co.edu.proyecto.orden.dominio;

/**
 * Patrón Strategy: regla intercambiable de descuento sobre el subtotal.
 *
 * <p>Contrato (Liskov): toda implementación, ante un subtotal válido (no nulo),
 * <ul>
 *   <li>nunca devuelve {@code null};</li>
 *   <li>nunca lanza una excepción;</li>
 *   <li>nunca devuelve un {@link Dinero} negativo;</li>
 *   <li>devuelve un monto en la misma moneda que recibió.</li>
 * </ul>
 */
public interface EstrategiaDescuento {

    Dinero aplicar(Dinero subtotal);
}
