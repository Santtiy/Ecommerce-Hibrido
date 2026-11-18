package co.edu.proyecto.orden.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS para POST /order. */
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
