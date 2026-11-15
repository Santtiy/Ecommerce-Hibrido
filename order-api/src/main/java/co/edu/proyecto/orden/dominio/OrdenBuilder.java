package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Builder: acumula los datos y valida todo en {@link #construir()},
 * de modo que nunca exista una orden a medio construir.
 */
public class OrdenBuilder {

    private String cliente;
    private final List<ItemOrden> items = new ArrayList<>();

    public OrdenBuilder cliente(String cliente) {
        this.cliente = cliente;
        return this;
    }

    public OrdenBuilder agregarItem(ItemOrden item) {
        if (item == null) {
            throw new OrdenInvalidaException("El ítem no puede ser nulo");
        }
        this.items.add(item);
        return this;
    }

    public Orden construir() {
        if (cliente == null || cliente.isBlank()) {
            throw new OrdenInvalidaException("El cliente de la orden es obligatorio");
        }
        if (items.isEmpty()) {
            throw new OrdenInvalidaException("La orden debe tener al menos un ítem"); // RN-01
        }
        // La identidad se genera aquí, antes de persistir: el dominio no depende de la BD.
        return new Orden(OrdenId.nuevo(), cliente, items);
    }
}
