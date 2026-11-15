package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.dominio.Orden;

import java.math.BigDecimal;

public record RespuestaOrden(String orderId, String estado, BigDecimal total, String moneda) {

    public static RespuestaOrden desde(Orden orden) {
        return new RespuestaOrden(
                orden.id().valor().toString(),
                orden.estado().name(),
                orden.total().monto(),
                orden.total().moneda());
    }
}
