package co.edu.proyecto.dashboard.estado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositorioEstadosTest {

    @Test
    void losTresResultadosSobreLaMismaOrdenProducenElEstadoCompleto() {
        // Arrange
        RepositorioEstados repositorio = new RepositorioEstados();

        // Act
        repositorio.registrarPago("orden-1", "PAYMENT_SUCCESS");
        repositorio.registrarStock("orden-1", "STOCK_RESERVED");
        repositorio.registrarEmail("orden-1", "EMAIL_SENT");

        // Assert
        assertEquals(new EstadoDashboard("PAYMENT_SUCCESS", "STOCK_RESERVED", "EMAIL_SENT"),
                repositorio.obtener("orden-1"));
    }

    @Test
    void recibirDosVecesElMismoResultadoNoCambiaNada() {
        // Arrange
        RepositorioEstados repositorio = new RepositorioEstados();
        EstadoDashboard primeraVez = repositorio.registrarPago("orden-1", "PAYMENT_FAILED");

        // Act
        EstadoDashboard segundaVez = repositorio.registrarPago("orden-1", "PAYMENT_FAILED");

        // Assert
        assertEquals(primeraVez, segundaVez);
        assertEquals(new EstadoDashboard("PAYMENT_FAILED", "pending", "pending"),
                repositorio.obtener("orden-1"));
        assertEquals(1, repositorio.todos().size());
    }
}
