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
        // Orígenes explícitos y confiables (no usar "*" ni patrones).
        configuration.setAllowedOrigins(List.of(
                "https://lepipae.github.io",
                "https://gameboxd-2nio.onrender.com",
                "http://localhost:3000",
                "http://localhost:5500",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5500"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept"));
        configuration.setAllowCredentials(false);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}