package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento: una orden fue registrada. Dato plano de transporte entre microservicios. */
public record OrdenCreada(String orderId, String cliente, String item, int cantidad,
                          BigDecimal monto, String moneda, Instant ocurridoEn) {
}
