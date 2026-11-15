package co.edu.proyecto.orden.infraestructura.rest;

import java.time.Instant;
import java.util.List;

public record RespuestaError(int codigo, String mensaje, List<String> detalles, Instant momento) {

    public RespuestaError {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }
}
