package co.edu.proyecto.analytics.so;

import org.springframework.stereotype.Component;

/**
 * Medidas de memoria y CPU de la JVM, obtenidas con Runtime.getRuntime().
 *
 * <ul>
 *   <li><b>total</b> (totalMemory): memoria que la JVM ya le pidió al sistema operativo para el
 *       heap. Crece, hasta el máximo, cuando hace falta.</li>
 *   <li><b>libre</b> (freeMemory): parte de ese total que todavía no ocupa ningún objeto.</li>
 *   <li><b>usada</b> = total − libre. Runtime no ofrece este dato directamente: lo ocupado es lo
 *       que la JVM ya reservó menos lo que sigue disponible dentro de esa reserva.</li>
 *   <li><b>máxima</b> (maxMemory): techo del heap (-Xmx). Superarlo produce OutOfMemoryError.</li>
 *   <li><b>procesadores</b>: núcleos que el sistema operativo pone a disposición de la JVM
 *       (en un contenedor con límite de CPU, los del límite).</li>
 * </ul>
 */
@Component
public class MonitorRecursos {

    private static final long MB = 1024L * 1024L;

    private final Runtime runtime = Runtime.getRuntime();

    public long memoriaUsadaMb() {
        return (runtime.totalMemory() - runtime.freeMemory()) / MB;
    }

    public long memoriaLibreMb() {
        return runtime.freeMemory() / MB;
    }

    public long memoriaTotalMb() {
        return runtime.totalMemory() / MB;
    }

    public long memoriaMaximaMb() {
        return runtime.maxMemory() / MB;
    }

    public int procesadoresDisponibles() {
        return runtime.availableProcessors();
    }

    /**
     * Reserva un bloque de memoria y lo devuelve. Mientras el llamador conserve la referencia, el
     * recolector no puede liberarlo, así que la memoria usada sube de forma observable.
     */
    public byte[] reservarBloque(int megas) {
        if (megas <= 0) {
            throw new IllegalArgumentException("Los megas deben ser positivos: " + megas);
        }
        return new byte[Math.toIntExact(megas * MB)];
    }

    public String reportar() {
        return String.format("memoria usada=%d MB, libre=%d MB, total=%d MB, máxima=%d MB, procesadores=%d",
                memoriaUsadaMb(), memoriaLibreMb(), memoriaTotalMb(), memoriaMaximaMb(), procesadoresDisponibles());
    }
}
