package co.edu.proyecto.analytics.so;

import org.springframework.stereotype.Component;

/** Medidas de memoria y CPU de la JVM, obtenidas con Runtime.getRuntime(). */
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
