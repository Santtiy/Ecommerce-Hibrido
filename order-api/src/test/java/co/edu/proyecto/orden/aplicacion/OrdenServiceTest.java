package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.aplicacion.dto.CrearOrdenRequest;
import co.edu.proyecto.orden.aplicacion.dto.ItemRequest;
import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.EstrategiaDescuento;
import co.edu.proyecto.orden.dominio.EstrategiaImpuesto;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import co.edu.proyecto.orden.dominio.puertos.PublicadorEventos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository repositorio;

    @Mock
    private PublicadorEventos publicador;

    @Mock
    private EstrategiaDescuento descuento;

    @Mock
    private EstrategiaImpuesto impuesto;

    @InjectMocks
    private OrdenService servicio;

    private static CrearOrdenRequest solicitudValida() {
        return new CrearOrdenRequest("Ana Pérez", List.of(
                new ItemRequest("SKU-1", "Monitor", 2, new BigDecimal("30000")),
                new ItemRequest("SKU-2", "Mouse", 1, new BigDecimal("15000"))));
    }

    private void estrategiasNeutras() {
        when(descuento.aplicar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(impuesto.aplicar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ordenServiceGuardaLaOrdenEnElRepositorioYPublicaElEvento() {
        estrategiasNeutras();
        when(repositorio.guardar(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<OrdenCreada> eventoCaptor = ArgumentCaptor.forClass(OrdenCreada.class);

        Orden orden = servicio.registrar(solicitudValida());

        verify(repositorio).guardar(orden);
        verify(publicador).publicar(eventoCaptor.capture());
        OrdenCreada evento = eventoCaptor.getValue();
        assertEquals(orden.id().valor().toString(), evento.orderId());
        assertEquals(new BigDecimal("75000.00"), evento.monto());
    }

    @Test
    void ordenServiceNoPublicaNingunEventoSiElRepositorioLanzaUnaExcepcion() {
        estrategiasNeutras();
        RuntimeException falloBd = new RuntimeException("Base de datos no disponible");
        doThrow(falloBd).when(repositorio).guardar(any());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> servicio.registrar(solicitudValida()));

        assertSame(falloBd, error);
        verifyNoInteractions(publicador);
    }

    @Test
    void ordenServiceDelegaElCalculoEnLasEstrategiasExactamenteUnaVez() {
        estrategiasNeutras();
        when(repositorio.guardar(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.registrar(solicitudValida());

        verify(descuento, times(1)).aplicar(Dinero.de("75000", "COP"));
        verify(impuesto, times(1)).aplicar(any(Dinero.class));
    }
}
