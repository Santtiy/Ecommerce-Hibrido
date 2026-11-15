package co.edu.proyecto.orden.aplicacion.dto;

import java.math.BigDecimal;

public record ItemRequest(String sku, String nombre, int cantidad, BigDecimal precioUnitario) {
}
