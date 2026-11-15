package co.edu.proyecto.analytics.so;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Acceso al sistema de archivos: registro de órdenes en un archivo de texto, solo por anexión.
 * Usa java.nio.file.Files, que delega en las llamadas al sistema operativo (open, write, close).
 */
@Component
public class GestorArchivos {

    private static final Logger log = LoggerFactory.getLogger(GestorArchivos.class);

    private final Path ruta;

    public GestorArchivos(@Value("${analytics.archivo-ordenes:./datos/ordenes.log}") String ruta) {
        this.ruta = Path.of(ruta).toAbsolutePath().normalize();
        try {
            Path directorio = this.ruta.getParent();
            if (directorio != null) {
                Files.createDirectories(directorio);   // no falla si ya existe
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de " + this.ruta, e);
        }
        log.info("Registro de órdenes en {}", this.ruta);
    }

    /**
     * Añade una línea al final del archivo y mide bytes escritos y tiempo empleado.
     * synchronized: varios hilos del consumidor escriben a la vez, y el método es la sección
     * crítica que impide que dos líneas se intercalen dentro del mismo archivo.
     */
    public synchronized ResultadoES escribir(String linea) {
        byte[] datos = (linea + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        long inicio = System.nanoTime();
        try {
            Files.write(ruta, datos, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en " + ruta, e);
        }
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        return new ResultadoES("escritura", datos.length, ms);
    }

    public synchronized List<String> leerTodo() {
        if (!Files.exists(ruta)) {
            return List.of();
        }
        try {
            return Files.readAllLines(ruta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }

    public long tamanoActual() {
        try {
            return Files.exists(ruta) ? Files.size(ruta) : 0L;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo consultar el tamaño de " + ruta, e);
        }
    }
}
