package co.edu.proyecto.orden.dominio;

/** Patrón Strategy. */
public interface EstrategiaImpuesto {

    Dinero aplicar(Dinero base);
}
