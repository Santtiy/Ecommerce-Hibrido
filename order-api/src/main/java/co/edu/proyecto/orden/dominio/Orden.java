package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad rica: protege sus invariantes. No tiene setters; el estado solo cambia
 * mediante métodos de negocio. Solo {@link OrdenBuilder} puede crearla.
 */
public class Orden {

    private final OrdenId id;
    private final String cliente;
    private final List<ItemOrden> items;
    private EstadoOrden estado;
    private Dinero total;

    Orden(OrdenId id, String cliente, List<ItemOrden> items) {
        this.id = id;
        this.cliente = cliente;
        this.items = new ArrayList<>(items);
        this.estado = EstadoOrden.CREADA;
        this.total = Dinero.cero(items.get(0).precioUnitario().moneda());
    }

    public void agregarProducto(ItemOrden item) {
        if (estado != EstadoOrden.CREADA) {
            throw new EstadoInvalidoException(
                    "Solo se pueden agregar productos a una orden CREADA; estado actual: " + estado);
        }
        if (item == null) {
            throw new OrdenInvalidaException("El ítem no puede ser nulo");
        }
        items.add(item);
    }

    /** RN-04: subtotal de ítems, luego descuento, luego impuesto. Guarda y devuelve el total. */
    public Dinero calcularTotal(EstrategiaDescuento descuento, EstrategiaImpuesto impuesto) {
        if (descuento == null || impuesto == null) {
            throw new OrdenInvalidaException("Las estrategias de descuento e impuesto son obligatorias");
        }
        Dinero subtotal = Dinero.cero(items.get(0).precioUnitario().moneda());
        for (ItemOrden item : items) {
            subtotal = subtotal.sumar(item.subtotal());
        }
        Dinero conDescuento = descuento.aplicar(subtotal);
        this.total = impuesto.aplicar(conDescuento);
        return total;
    }

    /** RN-06: solo una orden CREADA puede pagarse. */
    public void confirmarPago() {
        if (estado != EstadoOrden.CREADA) {
            throw new EstadoInvalidoException(
                    "Solo se puede confirmar el pago de una orden CREADA; estado actual: " + estado);
        }
        estado = EstadoOrden.PAGADA;
    }

    /** RN-07: una orden pagada no puede cancelarse. */
    public void cancelar() {
        if (estado == EstadoOrden.PAGADA) {
            throw new EstadoInvalidoException("Una orden pagada no puede cancelarse");
        }
        if (estado != EstadoOrden.CREADA) {
            throw new EstadoInvalidoException(
                    "Solo se puede cancelar una orden CREADA; estado actual: " + estado);
        }
        estado = EstadoOrden.CANCELADA;
    }

    public OrdenId id() {
        return id;
    }

    public String cliente() {
        return cliente;
    }

    public EstadoOrden estado() {
        return estado;
    }

    public Dinero total() {
        return total;
    }

    public List<ItemOrden> items() {
        return List.copyOf(items);
    }
}
