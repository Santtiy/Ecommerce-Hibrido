package co.edu.proyecto.dashboard.estado;

/**
 * Estado de una orden tal como lo muestra el dashboard. Se llama EstadoDashboard (y vive en su
 * propio paquete) para no confundirse con el enum EstadoOrden del dominio de order-api.
 * Inmutable: cada cambio devuelve una copia nueva.
 */
public record EstadoDashboard(String payment, String stock, String email) {

    public static final String PENDIENTE = "pending";

    public static EstadoDashboard inicial() {
        return new EstadoDashboard(PENDIENTE, PENDIENTE, PENDIENTE);
    }

    public EstadoDashboard conPago(String payment) {
        return new EstadoDashboard(payment, stock, email);
    }

    public EstadoDashboard conStock(String stock) {
        return new EstadoDashboard(payment, stock, email);
    }

    public EstadoDashboard conEmail(String email) {
        return new EstadoDashboard(payment, stock, email);
    }
}
