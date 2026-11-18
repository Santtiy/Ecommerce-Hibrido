package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.aplicacion.dto.CrearOrdenRequest;
import co.edu.proyecto.orden.aplicacion.dto.ItemRequest;
import co.edu.proyecto.orden.dominio.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.IvaColombia;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import co.edu.proyecto.orden.dominio.puertos.PublicadorEventos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrdenServiceCalculoTest {

    private OrdenRepository repositorio;
    private PublicadorEventos publicador;
    private OrdenService servicio;

    @BeforeEach
    void configurar() {
        repositorio = mock(OrdenRepository.class);
        publicador = mock(PublicadorEventos.class);
        servicio = new OrdenService(repositorio, publicador, new DescuentoPorMonto(), new IvaColombia());
    }

    @Test
    void ordenDe150000AplicaDescuentoYLuegoIvaYPublicaEseMonto() {
        when(repositorio.guardar(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        CrearOrdenRequest solicitud = new CrearOrdenRequest("Ana Pérez", List.of(
                new ItemRequest("SKU-1", "Monitor", 1, new BigDecimal("150000"))));
        ArgumentCaptor<OrdenCreada> eventoCaptor = ArgumentCaptor.forClass(OrdenCreada.class);

        Orden orden = servicio.registrar(solicitud);

        assertEquals(Dinero.de("160650.00", "COP"), orden.total());
        verify(publicador).publicar(eventoCaptor.capture());
        assertEquals(new BigDecimal("160650.00"), eventoCaptor.getValue().monto());
    }
}
