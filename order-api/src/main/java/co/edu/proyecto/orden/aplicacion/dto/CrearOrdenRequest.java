package co.edu.proyecto.orden.aplicacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CrearOrdenRequest(
        @NotBlank String cliente,
        @NotEmpty @Valid List<ItemRequest> items) {

    public CrearOrdenRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
