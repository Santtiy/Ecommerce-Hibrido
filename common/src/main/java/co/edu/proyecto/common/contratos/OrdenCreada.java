package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento. */
public record OrdenCreada(String orderId, String cliente, String item, int cantidad,
                          BigDecimal monto, String moneda, Instant ocurridoEn) {
}
