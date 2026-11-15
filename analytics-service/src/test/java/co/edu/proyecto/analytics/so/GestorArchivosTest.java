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
        // Subdirectorio inexistente: comprueba de paso que el gestor lo crea.
        return new GestorArchivos(directorioTemporal.resolve("datos/ordenes.log").toString());
    }

    @Test
    void escribirDevuelveBytesMayoresQueCero() {
        // Arrange
        GestorArchivos gestor = gestorEnTemporal();

        // Act
        ResultadoES resultado = gestor.escribir("orden-1;160650.00;2026-11-14T10:00:00Z");

        // Assert
        assertTrue(resultado.bytes() > 0);
        assertEquals(resultado.bytes(), gestor.tamanoActual());
        assertEquals("escritura", resultado.operacion());
    }

    @Test
    void leerTodoRecuperaLaLineaEscrita() {
        // Arrange
        GestorArchivos gestor = gestorEnTemporal();
        String linea = "orden-1;160650.00;2026-11-14T10:00:00Z";
        gestor.escribir(linea);

        // Act
        List<String> lineas = gestor.leerTodo();

        // Assert
        assertEquals(List.of(linea), lineas);
    }
}
