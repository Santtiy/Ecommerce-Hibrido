package co.edu.proyecto.dashboard.estado;

/** Estado de una orden tal como lo muestra el dashboard. */
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
