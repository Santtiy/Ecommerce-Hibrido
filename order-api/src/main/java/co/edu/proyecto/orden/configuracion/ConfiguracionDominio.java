package co.edu.proyecto.orden.configuracion;

import co.edu.proyecto.orden.aplicacion.OrdenService;
import co.edu.proyecto.orden.dominio.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.EstrategiaDescuento;
import co.edu.proyecto.orden.dominio.EstrategiaImpuesto;
import co.edu.proyecto.orden.dominio.IvaColombia;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import co.edu.proyecto.orden.dominio.puertos.PublicadorEventos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla el dominio y la aplicación sin anotarlos con Spring: así el dominio sigue siendo
 * Java puro y la inyección por constructor queda visible en un único lugar.
 * Para cambiar de regla de descuento o de impuesto solo se cambia el bean (Strategy + OCP).
 */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public EstrategiaDescuento estrategiaDescuento() {
        return new DescuentoPorMonto();
    }

    @Bean
    public EstrategiaImpuesto estrategiaImpuesto() {
        return new IvaColombia();
    }

    @Bean
    public OrdenService ordenService(OrdenRepository repositorio, PublicadorEventos publicador,
                                     EstrategiaDescuento descuento, EstrategiaImpuesto impuesto) {
        return new OrdenService(repositorio, publicador, descuento, impuesto);
    }
}
