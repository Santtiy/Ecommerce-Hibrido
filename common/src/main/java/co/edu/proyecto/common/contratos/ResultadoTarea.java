package co.edu.proyecto.common.contratos;

import java.time.Instant;

/** Resultado de una tarea asíncrona sobre una orden. */
public record ResultadoTarea(String orderId, String status, String detalle, Instant ocurridoEn) {
}
