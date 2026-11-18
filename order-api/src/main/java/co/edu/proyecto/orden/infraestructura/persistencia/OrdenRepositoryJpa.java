package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adaptador. */
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Orden> buscarPorId(OrdenId id) {
        return springData.findById(id.valor()).map(mapper::aDominio);
    }
}
