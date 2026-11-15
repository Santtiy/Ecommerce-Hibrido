package co.edu.proyecto.orden.dominio;

public class SinImpuesto implements EstrategiaImpuesto {

    @Override
    public Dinero aplicar(Dinero base) {
        return base;
    }
}
