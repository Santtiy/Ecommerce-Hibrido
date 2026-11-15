package co.edu.proyecto.analytics.aplicacion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Acumula las ventas recibidas.
 *
 * <p>Por qué estructuras concurrentes: el listener corre con varios hilos (uno por partición),
 * y todos llaman a {@link #registrar} sobre esta misma instancia. Con un {@code BigDecimal total}
 * y un {@code long cantidad} normales, dos hilos podrían leer el mismo valor, sumar cada uno su
 * monto y escribir: uno de los dos incrementos se perdería sin ningún error (condición de carrera).
 * {@link AtomicReference#accumulateAndGet} reintenta la suma hasta aplicarla sin interferencias y
 * {@link LongAdder} reparte el conteo entre celdas para no bloquear a los hilos.
 *
 * <p>Total y cantidad se actualizan por separado: una lectura simultánea a una escritura puede
 * ver uno ya actualizado y el otro no, pero ningún incremento se pierde.
 */
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
