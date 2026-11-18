package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;
import java.time.Instant;

/** Comando. */
public record ProcesarOrden(String orderId, String item, int cantidad, BigDecimal monto,
                            Instant solicitadoEn) {
}
