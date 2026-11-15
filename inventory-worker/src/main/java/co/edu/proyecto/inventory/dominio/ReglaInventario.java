package co.edu.proyecto.inventory.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;

import java.time.Instant;

/** RN-12: no hay existencias para más de 10 unidades por orden. Clase pura, sin Spring. */
public class ReglaInventario {

    public static final int MAXIMO_UNIDADES = 10;
    public static final String EXITO = "STOCK_RESERVED";
    public static final String FALLO = "STOCK_OUT_OF_STOCK";

    public ResultadoTarea evaluar(ProcesarOrden comando) {
        if (comando.cantidad() > MAXIMO_UNIDADES) {
            return new ResultadoTarea(comando.orderId(), FALLO,
                    "Se pidieron " + comando.cantidad() + " unidades de " + comando.item()
                            + "; el máximo disponible es " + MAXIMO_UNIDADES,
                    Instant.now());
        }
        return new ResultadoTarea(comando.orderId(), EXITO,
                comando.cantidad() + " unidades de " + comando.item() + " reservadas", Instant.now());
    }
}
