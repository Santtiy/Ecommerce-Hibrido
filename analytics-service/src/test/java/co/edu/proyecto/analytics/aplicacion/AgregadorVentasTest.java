package co.edu.proyecto.analytics.aplicacion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgregadorVentasTest {

    @Test
    void acumulaCorrectamenteTresMontosSucesivos() {
        AgregadorVentas agregador = new AgregadorVentas();

        agregador.registrar(new BigDecimal("160650.00"));
        agregador.registrar(new BigDecimal("59500.00"));
        agregador.registrar(new BigDecimal("1190.50"));

        assertEquals(new BigDecimal("221340.50"), agregador.totalAcumulado());
        assertEquals(3, agregador.cantidadOrdenes());
    }

    @Test
    void desde50HilosConcurrentesQueRegistran1000CadaUnoElTotalEsExacto() throws InterruptedException {
        int hilos = 50;
        int registrosPorHilo = 1000;
        AgregadorVentas agregador = new AgregadorVentas();
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);

        for (int h = 0; h < hilos; h++) {
            ejecutor.submit(() -> {
                salida.await();
                for (int i = 0; i < registrosPorHilo; i++) {
                    agregador.registrar(new BigDecimal("10.00"));
                }
                return null;
            });
        }
        salida.countDown();
        ejecutor.shutdown();
        boolean terminaron = ejecutor.awaitTermination(30, TimeUnit.SECONDS);

        assertTrue(terminaron);
        assertEquals(50_000, agregador.cantidadOrdenes());
        assertEquals(new BigDecimal("500000.00"), agregador.totalAcumulado());
    }
}
