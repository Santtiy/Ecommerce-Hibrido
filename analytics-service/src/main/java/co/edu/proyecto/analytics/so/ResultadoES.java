package co.edu.proyecto.analytics.so;

/** Resultado de una operación de entrada/salida: qué se hizo, cuántos bytes y cuánto tardó. */
public record ResultadoES(String operacion, long bytes, long milisegundos) {
}
