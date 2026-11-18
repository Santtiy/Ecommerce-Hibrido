package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

import java.util.ArrayList;
import java.util.List;

/** Patrón Builder. */
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
            throw new OrdenInvalidaException("La orden debe tener al menos un ítem");
        }
        return new Orden(OrdenId.nuevo(), cliente, items);
    }
}
