package co.edu.proyecto.orden.aplicacion.dto;

import java.util.List;

public record CrearOrdenRequest(String cliente, List<ItemRequest> items) {

    /** Copia defensiva: el record no debe compartir una lista mutable con quien lo crea. */
    public CrearOrdenRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
