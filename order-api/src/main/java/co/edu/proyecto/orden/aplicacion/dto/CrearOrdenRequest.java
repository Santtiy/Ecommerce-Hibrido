package co.edu.proyecto.orden.aplicacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CrearOrdenRequest(
        @NotBlank String cliente,
        @NotEmpty @Valid List<ItemRequest> items) {

    /** Copia defensiva: el record no debe compartir una lista mutable con quien lo crea. */
    public CrearOrdenRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
