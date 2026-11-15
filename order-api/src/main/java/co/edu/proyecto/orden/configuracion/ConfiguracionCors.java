package co.edu.proyecto.orden.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para POST /order: el dashboard se sirve desde dashboard-aggregator (puerto 8081) y envía
 * las órdenes a order-api (puerto 8080). Son orígenes distintos, así que sin esta configuración
 * el navegador bloquea la petición antes de que llegue al controlador.
 *
 * <p>Por defecto se permite cualquier origen ({@code *}) porque es una herramienta académica de
 * uso local: el dashboard puede abrirse desde localhost, 127.0.0.1 o la IP de la máquina virtual.
 * En un sistema real se restringiría al dominio del front, p. ej. {@code CORS_ORIGINS=https://tienda.com}.
 * Admite varios orígenes separados por comas.
 */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    private final String[] origenes;

    public ConfiguracionCors(@Value("${app.cors.origins}") String[] origenes) {
        this.origenes = origenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/order")
                .allowedOrigins(origenes)
                .allowedMethods("POST")
                .allowedHeaders("Content-Type");
    }
}
