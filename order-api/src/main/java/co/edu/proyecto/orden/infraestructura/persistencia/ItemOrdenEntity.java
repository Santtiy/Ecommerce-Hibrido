package co.edu.proyecto.orden.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Modelo de PERSISTENCIA de la tabla "items_orden", no modelo de dominio.
 * Constructor vacío y setters existen solo porque JPA los exige; las reglas del ítem
 * viven en el record {@code dominio.ItemOrden}.
 */
@Entity
@Table(name = "items_orden")
public class ItemOrdenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenEntity orden;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_monto", precision = 19, scale = 2)
    private BigDecimal precioMonto;

    @Column(name = "precio_moneda", columnDefinition = "char(3)")
    private String precioMoneda;

    protected ItemOrdenEntity() {
    }

    public Long getId() {
        return id;
    }

    public OrdenEntity getOrden() {
        return orden;
    }

    void setOrden(OrdenEntity orden) {
        this.orden = orden;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioMonto() {
        return precioMonto;
    }

    public void setPrecioMonto(BigDecimal precioMonto) {
        this.precioMonto = precioMonto;
    }

    public String getPrecioMoneda() {
        return precioMoneda;
    }

    public void setPrecioMoneda(String precioMoneda) {
        this.precioMoneda = precioMoneda;
    }
}
