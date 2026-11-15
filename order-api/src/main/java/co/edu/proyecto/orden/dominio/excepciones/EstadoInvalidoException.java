package co.edu.proyecto.orden.dominio.excepciones;

/** Se intentó una operación no permitida en el estado actual de la orden. */
public class EstadoInvalidoException extends DominioException {

    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
