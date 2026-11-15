package co.edu.proyecto.orden.dominio;

/** IVA general de Colombia: suma el 19 % sobre la base. */
public class IvaColombia implements EstrategiaImpuesto {

    private static final int TARIFA = 19;

    @Override
    public Dinero aplicar(Dinero base) {
        return base.sumar(base.porcentaje(TARIFA));
    }
}
