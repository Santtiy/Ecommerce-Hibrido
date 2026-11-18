package co.edu.proyecto.orden.dominio.puertos;

import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;

import java.util.Optional;

/** Puerto (patrón Repository). */
public interface OrdenRepository {

    Orden guardar(Orden orden);

    Optional<Orden> buscarPorId(OrdenId id);
}
