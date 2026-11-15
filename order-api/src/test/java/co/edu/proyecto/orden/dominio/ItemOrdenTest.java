package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemOrdenTest {

    @Test
    void itemOrdenRechazaCantidadCero() {
        // Arrange
        Dinero precio = Dinero.de("10000", "COP");

        // Act + Assert
        assertThrows(OrdenInvalidaException.class,
                () -> new ItemOrden("SKU-1", "Teclado", 0, precio));
    }

    @Test
    void itemOrdenCalculaCorrectamenteSuSubtotal() {
        // Arrange
        ItemOrden item = new ItemOrden("SKU-1", "Teclado", 3, Dinero.de("25000.50", "COP"));

        // Act
        Dinero subtotal = item.subtotal();

        // Assert
        assertEquals(Dinero.de("75001.50", "COP"), subtotal);
    }
}
