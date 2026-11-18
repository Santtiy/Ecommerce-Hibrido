package co.edu.proyecto.orden.dominio.puertos;

import co.edu.proyecto.common.contratos.OrdenCreada;

/** Puerto de salida. */
public interface PublicadorEventos {

    void publicar(OrdenCreada evento);
}
