package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.aplicacion.OrdenService;
import co.edu.proyecto.orden.aplicacion.dto.CrearOrdenRequest;
import co.edu.proyecto.orden.dominio.Orden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    /**
     * 202 ACCEPTED y no 201 CREATED: la orden fue aceptada y guardada, pero su procesamiento
     * asíncrono (eventos, pago, inventario) todavía no ha terminado.
     */
    @PostMapping("/order")
    public ResponseEntity<RespuestaOrden> crear(@Valid @RequestBody CrearOrdenRequest solicitud) {
        Orden orden = ordenService.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RespuestaOrden.desde(orden));
    }
}
