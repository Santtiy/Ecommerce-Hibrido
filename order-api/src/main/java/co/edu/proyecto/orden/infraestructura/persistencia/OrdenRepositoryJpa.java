package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador: implementa el puerto de dominio {@link OrdenRepository} con JPA.
 * Es el único punto donde el dominio y la base de datos se encuentran.
 */
@Repository
public class OrdenRepositoryJpa implements OrdenRepository {

    private final SpringDataOrdenRepository springData;
    private final OrdenMapper mapper;

    public OrdenRepositoryJpa(SpringDataOrdenRepository springData, OrdenMapper mapper) {
        this.springData = springData;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Orden guardar(Orden orden) {
        OrdenEntity guardada = springData.save(mapper.aEntidad(orden));
        return mapper.aDominio(guardada);
    }

    /** Transaccional para que la colección perezosa de ítems se cargue antes de mapear. */
    @Override
    @Transactional(readOnly = true)
    public Optional<Orden> buscarPorId(OrdenId id) {
        return springData.findById(id.valor()).map(mapper::aDominio);
    }
}
