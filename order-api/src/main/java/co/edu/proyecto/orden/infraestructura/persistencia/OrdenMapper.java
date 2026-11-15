package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.EstadoOrden;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import org.springframework.stereotype.Component;

import java.util.List;

/** Traduce entre el modelo de dominio y el modelo de persistencia, en ambos sentidos. */
@Component
public class OrdenMapper {

    public OrdenEntity aEntidad(Orden orden) {
        OrdenEntity entidad = new OrdenEntity();
        entidad.setId(orden.id().valor());
        entidad.setCliente(orden.cliente());
        entidad.setEstado(orden.estado().name());
        entidad.setTotalMonto(orden.total().monto());
        entidad.setTotalMoneda(orden.total().moneda());
        for (ItemOrden item : orden.items()) {
            entidad.agregarItem(aEntidad(item));
        }
        return entidad;
    }

    public Orden aDominio(OrdenEntity entidad) {
        List<ItemOrden> items = entidad.getItems().stream().map(this::aDominio).toList();
        return Orden.reconstituir(
                new OrdenId(entidad.getId()),
                entidad.getCliente(),
                items,
                EstadoOrden.valueOf(entidad.getEstado()),
                new Dinero(entidad.getTotalMonto(), entidad.getTotalMoneda()));
    }

    private ItemOrdenEntity aEntidad(ItemOrden item) {
        ItemOrdenEntity entidad = new ItemOrdenEntity();
        entidad.setSku(item.sku());
        entidad.setNombre(item.nombre());
        entidad.setCantidad(item.cantidad());
        entidad.setPrecioMonto(item.precioUnitario().monto());
        entidad.setPrecioMoneda(item.precioUnitario().moneda());
        return entidad;
    }

    private ItemOrden aDominio(ItemOrdenEntity entidad) {
        return new ItemOrden(
                entidad.getSku(),
                entidad.getNombre(),
                entidad.getCantidad(),
                new Dinero(entidad.getPrecioMonto(), entidad.getPrecioMoneda()));
    }
}
