package co.edu.proyecto.orden.infraestructura.persistencia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de PERSISTENCIA de la tabla "ordenes", no modelo de dominio.
 * Tiene constructor vacío y setters porque JPA los necesita; no es un modelo anémico
 * porque no representa al negocio: las reglas viven en {@code dominio.Orden} y
 * {@link OrdenMapper} traduce entre ambos.
 *
 * <p>creadaEn vive solo en persistencia porque es metadato de auditoría y ninguna
 * regla de negocio depende de él.
 */
@Entity
@Table(name = "ordenes")
public class OrdenEntity {

    /** Sin @GeneratedValue: el id lo genera el dominio (OrdenBuilder) antes de persistir. */
    @Id
    private UUID id;

    @Column(nullable = false)
    private String cliente;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "total_monto", precision = 19, scale = 2)
    private BigDecimal totalMonto;

    @Column(name = "total_moneda", columnDefinition = "char(3)")
    private String totalMoneda;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrdenEntity> items = new ArrayList<>();

    protected OrdenEntity() {
    }

    @PrePersist
    void alCrear() {
        this.creadaEn = Instant.now();
    }

    /** Mantiene las dos puntas de la relación sincronizadas. */
    public void agregarItem(ItemOrdenEntity item) {
        item.setOrden(this);
        items.add(item);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotalMonto() {
        return totalMonto;
    }

    public void setTotalMonto(BigDecimal totalMonto) {
        this.totalMonto = totalMonto;
    }

    public String getTotalMoneda() {
        return totalMoneda;
    }

    public void setTotalMoneda(String totalMoneda) {
        this.totalMoneda = totalMoneda;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public List<ItemOrdenEntity> getItems() {
        return items;
    }
}
