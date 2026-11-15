package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.dominio.excepciones.DominioException;
import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> datosInvalidos(MethodArgumentNotValidException ex) {
        List<String> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "La solicitud tiene campos inválidos", campos);
    }

    /** JSON mal formado o tipos incorrectos: es un error del cliente, no del servidor. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", List.of());
    }

    @ExceptionHandler(OrdenInvalidaException.class)
    public ResponseEntity<RespuestaError> ordenInvalida(OrdenInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<RespuestaError> estadoInvalido(EstadoInvalidoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    /** Cualquier otra regla de dominio violada (p. ej. en Dinero) es un dato inválido, no un fallo interno. */
    @ExceptionHandler(DominioException.class)
    public ResponseEntity<RespuestaError> reglaDeDominio(DominioException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    /** Último recurso: nunca expone el stack trace al cliente, solo lo registra en el log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex) {
        // Errores propios de Spring MVC (404, 405, 415...) conservan su código HTTP.
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatusCode estado = errorSpring.getStatusCode();
            return ResponseEntity.status(estado).body(new RespuestaError(
                    estado.value(), errorSpring.getBody().getDetail(), List.of(), Instant.now()));
        }
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus estado, String mensaje, List<String> detalles) {
        return ResponseEntity.status(estado)
                .body(new RespuestaError(estado.value(), mensaje, detalles, Instant.now()));
    }
}
