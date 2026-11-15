package co.edu.proyecto.orden.dominio.excepciones;

/** Excepción base para toda violación de una regla de negocio del dominio. */
public class DominioException extends RuntimeException {

    public DominioException(String mensaje) {
        super(mensaje);
    }

    public DominioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
