package co.edu.proyecto.orden.dominio;

/**
 * Patrón Strategy: regla intercambiable de impuesto sobre la base gravable.
 *
 * <p>Contrato (Liskov): toda implementación, ante una base válida (no nula),
 * <ul>
 *   <li>nunca devuelve {@code null};</li>
 *   <li>nunca lanza una excepción;</li>
 *   <li>nunca devuelve un {@link Dinero} negativo;</li>
 *   <li>devuelve un monto en la misma moneda que recibió.</li>
 * </ul>
 */
public interface EstrategiaImpuesto {

    Dinero aplicar(Dinero base);
}
