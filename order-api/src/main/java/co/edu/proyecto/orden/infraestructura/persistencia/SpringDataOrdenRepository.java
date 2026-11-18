package co.edu.proyecto.orden.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio técnico de Spring Data. */
public interface SpringDataOrdenRepository extends JpaRepository<OrdenEntity, UUID> {
}
