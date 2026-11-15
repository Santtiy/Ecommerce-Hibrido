package co.edu.proyecto.orden.dominio.puertos;

import co.edu.proyecto.common.contratos.OrdenCreada;

/** Puerto de salida: publicación de eventos. La tecnología de mensajería se decide fuera del dominio. */
public interface PublicadorEventos {

    void publicar(OrdenCreada evento);
}
