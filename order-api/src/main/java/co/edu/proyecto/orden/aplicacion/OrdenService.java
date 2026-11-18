package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.aplicacion.dto.CrearOrdenRequest;
import co.edu.proyecto.orden.aplicacion.dto.ItemRequest;
import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.EstrategiaDescuento;
import co.edu.proyecto.orden.dominio.EstrategiaImpuesto;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenBuilder;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import co.edu.proyecto.orden.dominio.puertos.PublicadorEventos;

import java.time.Instant;
import java.util.Objects;

/** Caso de uso. */
public class OrdenService {

    private static final String MONEDA = "COP";

    private final OrdenRepository repositorio;
    private final PublicadorEventos publicador;
    private final EstrategiaDescuento descuento;
    private final EstrategiaImpuesto impuesto;

    public OrdenService(OrdenRepository repositorio, PublicadorEventos publicador,
                        EstrategiaDescuento descuento, EstrategiaImpuesto impuesto) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.publicador = Objects.requireNonNull(publicador, "publicador");
        this.descuento = Objects.requireNonNull(descuento, "descuento");
        this.impuesto = Objects.requireNonNull(impuesto, "impuesto");
    }

    public Orden registrar(CrearOrdenRequest solicitud) {
        if (solicitud == null) {
            throw new OrdenInvalidaException("La solicitud no puede ser nula");
        }

        OrdenBuilder builder = new OrdenBuilder().cliente(solicitud.cliente());
        for (ItemRequest item : solicitud.items()) {
            builder.agregarItem(aItemOrden(item));
        }
        Orden orden = builder.construir();

        orden.calcularTotal(descuento, impuesto);

        Orden guardada = repositorio.guardar(orden);

        publicador.publicar(aEvento(guardada));

        return guardada;
    }

    private ItemOrden aItemOrden(ItemRequest item) {
        return new ItemOrden(item.sku(), item.nombre(), item.cantidad(),
                new Dinero(item.precioUnitario(), MONEDA));
    }

    private OrdenCreada aEvento(Orden orden) {
        ItemOrden primerItem = orden.items().get(0);
        Dinero total = orden.total();
        return new OrdenCreada(
                orden.id().valor().toString(),
                orden.cliente(),
                primerItem.sku(),
                primerItem.cantidad(),
                total.monto(),
                total.moneda(),
                Instant.now());
    }
}
