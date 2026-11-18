package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.DominioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Value Object inmutable. */
public record Dinero(BigDecimal monto, String moneda) {

    private static final int ESCALA = 2;

    public Dinero {
        if (monto == null) {
            throw new DominioException("El monto no puede ser nulo");
        }
        if (monto.signum() < 0) {
            throw new DominioException("El monto no puede ser negativo: " + monto);
        }
        if (moneda == null || !moneda.matches("[A-Za-z]{3}")) {
            throw new DominioException("La moneda debe tener exactamente 3 letras: " + moneda);
        }
        monto = monto.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public static Dinero de(String monto, String moneda) {
        if (monto == null) {
            throw new DominioException("El monto no puede ser nulo");
        }
        try {
            return new Dinero(new BigDecimal(monto.trim()), moneda);
        } catch (NumberFormatException e) {
            throw new DominioException("Monto con formato inválido: " + monto, e);
        }
    }

    public static Dinero cero(String moneda) {
        return new Dinero(BigDecimal.ZERO, moneda);
    }

    public Dinero sumar(Dinero otro) {
        validarMismaMoneda(otro);
        return new Dinero(monto.add(otro.monto), moneda);
    }

    public Dinero restar(Dinero otro) {
        validarMismaMoneda(otro);
        BigDecimal resultado = monto.subtract(otro.monto);
        if (resultado.signum() < 0) {
            throw new DominioException("La resta produce un monto negativo: " + resultado);
        }
        return new Dinero(resultado, moneda);
    }

    public Dinero multiplicar(int factor) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(factor)), moneda);
    }

    public Dinero porcentaje(int porcentaje) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(porcentaje)).movePointLeft(2), moneda);
    }

    public boolean esMayorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return monto.compareTo(otro.monto) > 0;
    }

    private void validarMismaMoneda(Dinero otro) {
        if (otro == null) {
            throw new DominioException("El otro monto no puede ser nulo");
        }
        if (!moneda.equals(otro.moneda)) {
            throw new DominioException(
                    "No se pueden operar monedas distintas: " + moneda + " y " + otro.moneda);
        }
    }
}
