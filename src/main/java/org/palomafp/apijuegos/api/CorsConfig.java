package org.palomafp.apijuegos.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de CORS para que el frontend estático de GitHub Pages pueda
 * llamar a la API desde otro origen.
 * @author Andrés López
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        // List.of en vez de Arrays.asList: devuelve una lista inmutable, y así el
        // bean no puede ser modificado por otra parte de la aplicación.
        configuration.setAllowedOriginPatterns(List.of("*")); // Permite cualquier origen
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*")); // Permite todas las cabeceras
        configuration.setAllowCredentials(false); // Seguridad relajada para el despliegue automático

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Aplica a todas las rutas
        return source;
    }
}
