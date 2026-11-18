package co.edu.proyecto.analytics.so;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorArchivosTest {

    @TempDir
    Path directorioTemporal;

    private GestorArchivos gestorEnTemporal() {
        return new GestorArchivos(directorioTemporal.resolve("datos/ordenes.log").toString());
    }

    @Test
    void escribirDevuelveBytesMayoresQueCero() {
        GestorArchivos gestor = gestorEnTemporal();

        ResultadoES resultado = gestor.escribir("orden-1;160650.00;2026-11-14T10:00:00Z");

        assertTrue(resultado.bytes() > 0);
        assertEquals(resultado.bytes(), gestor.tamanoActual());
        assertEquals("escritura", resultado.operacion());
    }

    @Test
    void leerTodoRecuperaLaLineaEscrita() {
        GestorArchivos gestor = gestorEnTemporal();
        String linea = "orden-1;160650.00;2026-11-14T10:00:00Z";
        gestor.escribir(linea);

        List<String> lineas = gestor.leerTodo();

        assertEquals(List.of(linea), lineas);
    }
}
