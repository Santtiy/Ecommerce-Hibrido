package co.edu.proyecto.orden.dominio.excepciones;

/** Los datos para construir una orden o sus ítems no cumplen las reglas de negocio. */
public class OrdenInvalidaException extends DominioException {

    public OrdenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
