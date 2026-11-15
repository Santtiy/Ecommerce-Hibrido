package co.edu.proyecto.orden.dominio.puertos;

import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;

import java.util.Optional;

/** Puerto (patrón Repository): el dominio define el contrato; la infraestructura lo implementará. */
public interface OrdenRepository {

    Orden guardar(Orden orden);

    Optional<Orden> buscarPorId(OrdenId id);
}
