package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.DominioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DineroTest {

    @Test
    void dineroRechazaUnMontoNegativo() {
        // Arrange
        BigDecimal negativo = new BigDecimal("-1.00");

        // Act + Assert
        assertThrows(DominioException.class, () -> new Dinero(negativo, "COP"));
    }

    @Test
    void dineroSumaDosCantidadesDeLaMismaMoneda() {
        // Arrange
        Dinero a = Dinero.de("1500.50", "COP");
        Dinero b = Dinero.de("499.50", "COP");

        // Act
        Dinero resultado = a.sumar(b);

        // Assert
        assertEquals(Dinero.de("2000.00", "COP"), resultado);
    }

    @Test
    void dineroRechazaSumarMonedasDistintas() {
        // Arrange
        Dinero pesos = Dinero.de("1000", "COP");
        Dinero dolares = Dinero.de("10", "USD");

        // Act + Assert
        assertThrows(DominioException.class, () -> pesos.sumar(dolares));
    }
}
