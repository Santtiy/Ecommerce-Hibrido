package co.edu.proyecto.analytics.aplicacion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/** Acumula las ventas recibidas. */
@Component
public class AgregadorVentas {

    private final AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO);
    private final LongAdder cantidad = new LongAdder();

    public void registrar(BigDecimal monto) {
        Objects.requireNonNull(monto, "monto");
        total.accumulateAndGet(monto, BigDecimal::add);
        cantidad.increment();
    }

    public BigDecimal totalAcumulado() {
        return total.get();
    }

    public long cantidadOrdenes() {
        return cantidad.sum();
    }
}
