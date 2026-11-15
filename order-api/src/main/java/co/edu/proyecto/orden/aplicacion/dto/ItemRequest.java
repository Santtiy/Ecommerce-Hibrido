package co.edu.proyecto.orden.aplicacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank String sku,
        @NotBlank String nombre,
        @Min(1) int cantidad,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precioUnitario) {
}
